package com.benzjeremy.wetter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherRepository {

    private static final String PREFS_NAME = "wetter_prefs";
    private static final String KEY_CACHE = "cached_weather";
    private static final String KEY_LOC_NAME = "loc_name";
    private static final String KEY_LOC_LAT = "loc_lat";
    private static final String KEY_LOC_LON = "loc_lon";
    private static final String KEY_UNIT = "temp_unit";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WeatherCallback {
        void onSuccess(WeatherData data);
        void onError(Exception e);
    }

    public static class CityResult {
        public String name;
        public double latitude;
        public double longitude;
        public String country;
        public String admin1;
    }

    public interface SearchCallback {
        void onSuccess(List<CityResult> results);
        void onError(Exception e);
    }

    public static class LocationInfo {
        public String name;
        public double latitude;
        public double longitude;

        public LocationInfo(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    // Load saved location, default: Siegen, NRW
    public static LocationInfo loadLocation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_LOC_NAME, "Siegen, NRW");
        double lat = Double.longBitsToDouble(prefs.getLong(KEY_LOC_LAT, Double.doubleToLongBits(50.8748)));
        double lon = Double.longBitsToDouble(prefs.getLong(KEY_LOC_LON, Double.doubleToLongBits(8.0243)));
        return new LocationInfo(name, lat, lon);
    }

    public static void saveLocation(Context context, String name, double lat, double lon) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LOC_NAME, name)
                .putLong(KEY_LOC_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LOC_LON, Double.doubleToRawLongBits(lon))
                .apply();
    }

    public static String getUnit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_UNIT, "celsius");
    }

    public static void setUnit(Context context, String unit) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_UNIT, unit).apply();
    }

    public static void saveCache(Context context, WeatherData data) {
        if (data == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CACHE, data.toJson()).apply();
    }

    public static WeatherData loadCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CACHE, null);
        if (json != null) {
            return WeatherData.fromJson(json);
        }
        return null;
    }

    // Direct HTTP connection to Open-Meteo Forecast API
    public static void fetchWeather(Context context, double lat, double lon, String locationName, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                String urlStr = "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover"
                        + "&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,shortwave_radiation_instant,cloud_cover"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum"
                        + "&timezone=auto";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Wetter-Android/1.0 (GPL-3.0; github.com/benzjeremy/wetter-site)");

                int code = conn.getResponseCode();
                if (code != 200) {
                    throw new Exception("HTTP error code: " + code);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JSONObject root = new JSONObject(sb.toString());
                WeatherData data = new WeatherData();
                data.locationName = locationName;
                data.latitude = lat;
                data.longitude = lon;
                data.timestamp = System.currentTimeMillis();

                // Current
                JSONObject cur = root.getJSONObject("current");
                data.currentTemp = cur.optDouble("temperature_2m", 0.0);
                data.apparentTemp = cur.optDouble("apparent_temperature", data.currentTemp);
                data.relativeHumidity = cur.optInt("relative_humidity_2m", 50);
                data.precipitation = cur.optDouble("precipitation", 0.0);
                data.weatherCode = cur.optInt("weather_code", 0);
                data.windSpeed = cur.optDouble("wind_speed_10m", 0.0);
                data.windDirection = cur.optInt("wind_direction_10m", 0);
                data.surfacePressure = cur.optDouble("surface_pressure", 1013.25);
                data.cloudCover = cur.optInt("cloud_cover", 0);
                data.isDay = cur.optInt("is_day", 1) == 1;

                // Hourly (take next 24 entries)
                JSONObject hourly = root.optJSONObject("hourly");
                double pvDailyKwh = 0.0;
                double currentRadiation = 0.0;
                if (hourly != null) {
                    JSONArray times = hourly.optJSONArray("time");
                    JSONArray temps = hourly.optJSONArray("temperature_2m");
                    JSONArray codes = hourly.optJSONArray("weather_code");
                    JSONArray rainProbs = hourly.optJSONArray("precipitation_probability");
                    JSONArray rads = hourly.optJSONArray("shortwave_radiation_instant");

                    if (times != null) {
                        int maxH = Math.min(24, times.length());
                        for (int i = 0; i < maxH; i++) {
                            WeatherData.HourlyItem item = new WeatherData.HourlyItem();
                            String rawTime = times.optString(i, "");
                            item.time = rawTime.contains("T") ? rawTime.substring(rawTime.indexOf("T") + 1) : rawTime;
                            item.temp = temps != null ? temps.optDouble(i, 0.0) : 0.0;
                            item.weatherCode = codes != null ? codes.optInt(i, 0) : 0;
                            item.rainProb = rainProbs != null ? rainProbs.optInt(i, 0) : 0;
                            item.radiation = rads != null ? rads.optDouble(i, 0.0) : 0.0;
                            data.hourly.add(item);

                            if (i == 0) {
                                currentRadiation = item.radiation;
                            }
                            // Hourly integration for estimated today yield
                            double hrKw = WeatherData.calcPvPower(item.radiation, item.temp);
                            pvDailyKwh += hrKw * 1.0;
                        }
                    }
                }

                // PV Calculation
                data.pvPowerKw = WeatherData.calcPvPower(currentRadiation, data.currentTemp);
                data.pvEnergyKwhToday = Math.round(pvDailyKwh * 10.0) / 10.0;

                // Daily
                JSONObject daily = root.optJSONObject("daily");
                if (daily != null) {
                    JSONArray dDates = daily.optJSONArray("time");
                    JSONArray dCodes = daily.optJSONArray("weather_code");
                    JSONArray dMaxs = daily.optJSONArray("temperature_2m_max");
                    JSONArray dMins = daily.optJSONArray("temperature_2m_min");
                    JSONArray dSunrise = daily.optJSONArray("sunrise");
                    JSONArray dSunset = daily.optJSONArray("sunset");
                    JSONArray dPrecip = daily.optJSONArray("precipitation_sum");

                    if (dDates != null) {
                        int maxD = Math.min(7, dDates.length());
                        for (int i = 0; i < maxD; i++) {
                            WeatherData.DailyItem item = new WeatherData.DailyItem();
                            item.date = dDates.optString(i, "");
                            item.weatherCode = dCodes != null ? dCodes.optInt(i, 0) : 0;
                            item.tempMax = dMaxs != null ? dMaxs.optDouble(i, 0.0) : 0.0;
                            item.tempMin = dMins != null ? dMins.optDouble(i, 0.0) : 0.0;
                            item.sunrise = dSunrise != null ? dSunrise.optString(i, "") : "";
                            item.sunset = dSunset != null ? dSunset.optString(i, "") : "";
                            item.precipSum = dPrecip != null ? dPrecip.optDouble(i, 0.0) : 0.0;
                            data.daily.add(item);
                        }
                    }
                }

                // Cache locally
                saveCache(context, data);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(data);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    // Direct HTTP Geocoding API search
    public static void searchCities(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String enc = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=" + enc + "&count=6&language=de&format=json";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    throw new Exception("HTTP geocoding error: " + code);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray results = root.optJSONArray("results");
                List<CityResult> list = new ArrayList<>();
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject r = results.getJSONObject(i);
                        CityResult cr = new CityResult();
                        cr.name = r.optString("name", "");
                        cr.latitude = r.optDouble("latitude", 0.0);
                        cr.longitude = r.optDouble("longitude", 0.0);
                        cr.country = r.optString("country", "");
                        cr.admin1 = r.optString("admin1", "");
                        list.add(cr);
                    }
                }

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(list);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }
}
