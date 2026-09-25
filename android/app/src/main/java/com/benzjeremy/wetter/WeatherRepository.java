package com.benzjeremy.wetter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
    private static final String KEY_LOC_NAME = "loc_name";
    private static final String KEY_LOC_LAT = "loc_lat";
    private static final String KEY_LOC_LON = "loc_lon";
    private static final String KEY_UNIT = "temp_unit";
    private static final String KEY_AUTO_REFRESH = "auto_refresh_interval";
    private static final String KEY_CACHE = "cached_weather";
    private static final String KEY_FAVORITES = "fav_cities_json";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WeatherCallback {
        void onSuccess(WeatherData data);
        void onError(Exception e);
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

    public static class CityResult {
        public String name;
        public double latitude;
        public double longitude;
        public String country;
        public String admin1;
    }

    public static class FavoriteCity {
        public String name;
        public double lat;
        public double lon;

        public FavoriteCity(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static LocationInfo loadLocation(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = sp.getString(KEY_LOC_NAME, "Siegen, NRW");
        double lat = Double.longBitsToDouble(sp.getLong(KEY_LOC_LAT, Double.doubleToLongBits(50.8748)));
        double lon = Double.longBitsToDouble(sp.getLong(KEY_LOC_LON, Double.doubleToLongBits(8.0243)));
        return new LocationInfo(name, lat, lon);
    }

    public static void saveLocation(Context context, String name, double lat, double lon) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_LOC_NAME, name)
                .putLong(KEY_LOC_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LOC_LON, Double.doubleToRawLongBits(lon))
                .apply();
    }

    public static List<FavoriteCity> getFavorites(Context context) {
        List<FavoriteCity> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_FAVORITES, null);
        if (json == null || json.trim().isEmpty()) {
            list.add(new FavoriteCity("Siegen, NRW", 50.8748, 8.0243));
            list.add(new FavoriteCity("Dortmund, NRW", 51.5136, 7.4653));
            list.add(new FavoriteCity("Berlin", 52.5200, 13.4050));
            saveFavorites(context, list);
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new FavoriteCity(
                        obj.optString("name", "Unbekannt"),
                        obj.optDouble("lat", 50.8748),
                        obj.optDouble("lon", 8.0243)
                ));
            }
        } catch (Exception ignored) {
            list.add(new FavoriteCity("Siegen, NRW", 50.8748, 8.0243));
        }
        return list;
    }

    public static void saveFavorites(Context context, List<FavoriteCity> list) {
        try {
            JSONArray arr = new JSONArray();
            for (FavoriteCity fc : list) {
                JSONObject obj = new JSONObject();
                obj.put("name", fc.name);
                obj.put("lat", fc.lat);
                obj.put("lon", fc.lon);
                arr.put(obj);
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_FAVORITES, arr.toString())
                    .apply();
        } catch (Exception ignored) {}
    }

    public static boolean isFavorite(Context context, String name) {
        if (name == null) return false;
        List<FavoriteCity> favs = getFavorites(context);
        for (FavoriteCity fc : favs) {
            if (name.equalsIgnoreCase(fc.name) || fc.name.toLowerCase().contains(name.toLowerCase()) || name.toLowerCase().contains(fc.name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static void addFavorite(Context context, String name, double lat, double lon) {
        List<FavoriteCity> favs = getFavorites(context);
        for (FavoriteCity fc : favs) {
            if (fc.name.equalsIgnoreCase(name)) return;
        }
        favs.add(new FavoriteCity(name, lat, lon));
        saveFavorites(context, favs);
    }

    public static void removeFavorite(Context context, String name) {
        List<FavoriteCity> favs = getFavorites(context);
        FavoriteCity toRemove = null;
        for (FavoriteCity fc : favs) {
            if (fc.name.equalsIgnoreCase(name) || fc.name.toLowerCase().contains(name.toLowerCase()) || name.toLowerCase().contains(fc.name.toLowerCase())) {
                toRemove = fc;
                break;
            }
        }
        if (toRemove != null) {
            favs.remove(toRemove);
            saveFavorites(context, favs);
        }
    }

    public static String getUnit(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_UNIT, "celsius");
    }

    public static void setUnit(Context context, String unit) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_UNIT, unit).apply();
    }

    public static int getAutoRefreshInterval(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_AUTO_REFRESH, 60);
    }

    public static void setAutoRefreshInterval(Context context, int intervalMinutes) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_AUTO_REFRESH, intervalMinutes).apply();
    }

    public static void scheduleAutoRefresh(Context context, int intervalMinutes) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, WeatherWidgetProvider.class);
        intent.setAction(WeatherWidgetProvider.ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT | 0x04000000);

        if (intervalMinutes <= 0) {
            am.cancel(pi);
        } else {
            long intervalMillis = (long) intervalMinutes * 60 * 1000;
            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + intervalMillis, intervalMillis, pi);
        }
    }

    public static void saveCache(Context context, WeatherData data) {
        if (data == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_CACHE, data.toJson()).apply();
    }

    public static WeatherData loadCache(Context context) {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CACHE, null);
        if (json != null) {
            return WeatherData.fromJson(json);
        }
        return null;
    }

    public static void fetchWeather(Context context, double lat, double lon, String locationName, WeatherCallback callback) {
        executor.execute(new FetchWeatherTask(context.getApplicationContext(), lat, lon, locationName, callback));
    }

    public static void searchCities(String query, SearchCallback callback) {
        executor.execute(new SearchCitiesTask(query, callback));
    }

    // Named static Runnables to avoid JDK 26 / D8 anonymous class NPE
    public static class PostSuccessRunnable implements Runnable {
        private final WeatherCallback callback;
        private final WeatherData data;

        public PostSuccessRunnable(WeatherCallback callback, WeatherData data) {
            this.callback = callback;
            this.data = data;
        }

        @Override
        public void run() {
            if (callback != null) {
                callback.onSuccess(data);
            }
        }
    }

    public static class PostErrorRunnable implements Runnable {
        private final WeatherCallback callback;
        private final Exception error;

        public PostErrorRunnable(WeatherCallback callback, Exception error) {
            this.callback = callback;
            this.error = error;
        }

        @Override
        public void run() {
            if (callback != null) {
                callback.onError(error);
            }
        }
    }

    public static class PostSearchSuccessRunnable implements Runnable {
        private final SearchCallback callback;
        private final List<CityResult> list;

        public PostSearchSuccessRunnable(SearchCallback callback, List<CityResult> list) {
            this.callback = callback;
            this.list = list;
        }

        @Override
        public void run() {
            if (callback != null) {
                callback.onSuccess(list);
            }
        }
    }

    public static class PostSearchErrorRunnable implements Runnable {
        private final SearchCallback callback;
        private final Exception error;

        public PostSearchErrorRunnable(SearchCallback callback, Exception error) {
            this.callback = callback;
            this.error = error;
        }

        @Override
        public void run() {
            if (callback != null) {
                callback.onError(error);
            }
        }
    }

    public static class FetchWeatherTask implements Runnable {
        private final Context context;
        private final double lat;
        private final double lon;
        private final String locationName;
        private final WeatherCallback callback;

        public FetchWeatherTask(Context context, double lat, double lon, String locationName, WeatherCallback callback) {
            this.context = context;
            this.lat = lat;
            this.lon = lon;
            this.locationName = locationName;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                        "&longitude=" + lon +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover" +
                        "&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,shortwave_radiation_instant,cloud_cover" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum" +
                        "&timezone=auto";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Wetter-Android/1.5 (GPL-3.0; https://pi5.darter-basking.ts.net/wetter-site/)");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new Exception("HTTP error code: " + responseCode);
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

                JSONObject current = root.getJSONObject("current");
                data.currentTemp = current.optDouble("temperature_2m", 0.0);
                data.apparentTemp = current.optDouble("apparent_temperature", data.currentTemp);
                data.relativeHumidity = current.optInt("relative_humidity_2m", 50);
                data.precipitation = current.optDouble("precipitation", 0.0);
                data.weatherCode = current.optInt("weather_code", 0);
                data.windSpeed = current.optDouble("wind_speed_10m", 0.0);
                data.windDirection = current.optInt("wind_direction_10m", 0);
                data.surfacePressure = current.optDouble("surface_pressure", 1013.25);
                data.cloudCover = current.optInt("cloud_cover", 0);
                data.isDay = current.optInt("is_day", 1) == 1;

                JSONObject hourly = root.optJSONObject("hourly");
                if (hourly != null) {
                    JSONArray times = hourly.optJSONArray("time");
                    JSONArray temps = hourly.optJSONArray("temperature_2m");
                    JSONArray codes = hourly.optJSONArray("weather_code");
                    JSONArray rainProbs = hourly.optJSONArray("precipitation_probability");
                    JSONArray radiations = hourly.optJSONArray("shortwave_radiation_instant");

                    if (times != null) {
                        int count = Math.min(24, times.length());
                        double totalPvEnergy = 0.0;
                        double currentRad = 0.0;

                        for (int i = 0; i < count; i++) {
                            WeatherData.HourlyItem item = new WeatherData.HourlyItem();
                            String rawTime = times.optString(i, "");
                            if (rawTime.contains("T")) {
                                item.time = rawTime.substring(rawTime.indexOf("T") + 1);
                            } else {
                                item.time = rawTime;
                            }
                            item.temp = temps != null ? temps.optDouble(i, 0.0) : 0.0;
                            item.weatherCode = codes != null ? codes.optInt(i, 0) : 0;
                            item.rainProb = rainProbs != null ? rainProbs.optInt(i, 0) : 0;
                            item.radiation = radiations != null ? radiations.optDouble(i, 0.0) : 0.0;
                            data.hourly.add(item);

                            if (i == 0) {
                                currentRad = item.radiation;
                            }
                            totalPvEnergy += WeatherData.calcPvPower(item.radiation, item.temp) * 1.0;
                        }
                        data.pvPowerKw = WeatherData.calcPvPower(currentRad, data.currentTemp);
                        data.pvEnergyKwhToday = Math.round(totalPvEnergy * 10.0) / 10.0;
                    }
                }

                JSONObject daily = root.optJSONObject("daily");
                if (daily != null) {
                    JSONArray dates = daily.optJSONArray("time");
                    JSONArray codes = daily.optJSONArray("weather_code");
                    JSONArray maxTemps = daily.optJSONArray("temperature_2m_max");
                    JSONArray minTemps = daily.optJSONArray("temperature_2m_min");
                    JSONArray sunrises = daily.optJSONArray("sunrise");
                    JSONArray sunsets = daily.optJSONArray("sunset");
                    JSONArray precips = daily.optJSONArray("precipitation_sum");

                    if (dates != null) {
                        int count = Math.min(7, dates.length());
                        for (int i = 0; i < count; i++) {
                            WeatherData.DailyItem item = new WeatherData.DailyItem();
                            item.date = dates.optString(i, "");
                            item.weatherCode = codes != null ? codes.optInt(i, 0) : 0;
                            item.tempMax = maxTemps != null ? maxTemps.optDouble(i, 0.0) : 0.0;
                            item.tempMin = minTemps != null ? minTemps.optDouble(i, 0.0) : 0.0;
                            item.sunrise = sunrises != null ? sunrises.optString(i, "") : "";
                            item.sunset = sunsets != null ? sunsets.optString(i, "") : "";
                            item.precipSum = precips != null ? precips.optDouble(i, 0.0) : 0.0;
                            data.daily.add(item);
                        }
                    }
                }

                saveCache(context, data);
                mainHandler.post(new PostSuccessRunnable(callback, data));
            } catch (Exception e) {
                mainHandler.post(new PostErrorRunnable(callback, e));
            }
        }
    }

    public static class SearchCitiesTask implements Runnable {
        private final String query;
        private final SearchCallback callback;

        public SearchCitiesTask(String query, SearchCallback callback) {
            this.query = query;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                        URLEncoder.encode(query, "UTF-8") +
                        "&count=6&language=de&format=json";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new Exception("HTTP geocoding error: " + responseCode);
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
                        JSONObject obj = results.getJSONObject(i);
                        CityResult cr = new CityResult();
                        cr.name = obj.optString("name", "");
                        cr.latitude = obj.optDouble("latitude", 0.0);
                        cr.longitude = obj.optDouble("longitude", 0.0);
                        cr.country = obj.optString("country", "");
                        cr.admin1 = obj.optString("admin1", "");
                        list.add(cr);
                    }
                }

                mainHandler.post(new PostSearchSuccessRunnable(callback, list));
            } catch (Exception e) {
                mainHandler.post(new PostSearchErrorRunnable(callback, e));
            }
        }
    }
}
