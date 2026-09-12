package com.benzjeremy.wetter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WeatherData {
    public String locationName;
    public double latitude;
    public double longitude;
    public long timestamp;

    // Current
    public double currentTemp;
    public double apparentTemp;
    public int relativeHumidity;
    public double precipitation;
    public int weatherCode;
    public double windSpeed;
    public int windDirection;
    public double surfacePressure;
    public int cloudCover;
    public boolean isDay;

    // Solar PV
    public double pvPowerKw;
    public double pvEnergyKwhToday;

    // Forecasts
    public List<HourlyItem> hourly = new ArrayList<>();
    public List<DailyItem> daily = new ArrayList<>();

    public static class HourlyItem {
        public String time;
        public double temp;
        public int weatherCode;
        public int rainProb;
        public double radiation;
    }

    public static class DailyItem {
        public String date;
        public int weatherCode;
        public double tempMax;
        public double tempMin;
        public String sunrise;
        public String sunset;
        public double precipSum;
    }

    // WMO Weather Mapping
    public static String getWeatherDescription(int code, boolean isDe) {
        switch (code) {
            case 0: return isDe ? "Klarer Himmel" : "Clear sky";
            case 1: return isDe ? "Überwiegend klar" : "Mainly clear";
            case 2: return isDe ? "Teilweise bewölkt" : "Partly cloudy";
            case 3: return isDe ? "Bedeckt" : "Overcast";
            case 45: return isDe ? "Nebel" : "Fog";
            case 48: return isDe ? "Reifnebel" : "Depositing rime fog";
            case 51: return isDe ? "Leichter Nieselregen" : "Light drizzle";
            case 53: return isDe ? "Mäßiger Nieselregen" : "Moderate drizzle";
            case 55: return isDe ? "Dichter Nieselregen" : "Dense drizzle";
            case 56:
            case 57: return isDe ? "Gefrierender Nieselregen" : "Freezing drizzle";
            case 61: return isDe ? "Leichter Regen" : "Slight rain";
            case 63: return isDe ? "Mäßiger Regen" : "Moderate rain";
            case 65: return isDe ? "Starker Regen" : "Heavy rain";
            case 66:
            case 67: return isDe ? "Gefrierender Regen" : "Freezing rain";
            case 71: return isDe ? "Leichter Schneefall" : "Slight snow fall";
            case 73: return isDe ? "Mäßiger Schneefall" : "Moderate snow fall";
            case 75: return isDe ? "Starker Schneefall" : "Heavy snow fall";
            case 77: return isDe ? "Schneegriesel" : "Snow grains";
            case 80: return isDe ? "Leichte Regenschauer" : "Slight rain showers";
            case 81: return isDe ? "Mäßige Regenschauer" : "Moderate rain showers";
            case 82: return isDe ? "Heftige Regenschauer" : "Violent rain showers";
            case 85:
            case 86: return isDe ? "Schneeschauer" : "Snow showers";
            case 95: return isDe ? "Gewitter" : "Thunderstorm";
            case 96:
            case 99: return isDe ? "Gewitter mit Hagel" : "Thunderstorm with hail";
            default: return isDe ? "Leicht bewölkt" : "Partly cloudy";
        }
    }

    public static String getWeatherIcon(int code) {
        switch (code) {
            case 0: return "☀️";
            case 1: return "🌤️";
            case 2: return "⛅";
            case 3: return "☁️";
            case 45:
            case 48: return "🌫️";
            case 51:
            case 53:
            case 55:
            case 56:
            case 57: return "🌦️";
            case 61:
            case 63:
            case 65:
            case 80:
            case 81:
            case 82: return "🌧️";
            case 66:
            case 67:
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86: return "❄️";
            case 95:
            case 96:
            case 99: return "⛈️";
            default: return "⛅";
        }
    }

    // Unit Conversion
    public static double convertTemp(double celsius, String unit) {
        if ("fahrenheit".equalsIgnoreCase(unit)) {
            return (celsius * 9.0 / 5.0) + 32.0;
        } else if ("kelvin".equalsIgnoreCase(unit)) {
            return celsius + 273.15;
        }
        return celsius;
    }

    public static String getUnitSymbol(String unit) {
        if ("fahrenheit".equalsIgnoreCase(unit)) return "°F";
        if ("kelvin".equalsIgnoreCase(unit)) return "K";
        return "°C";
    }

    public static String formatTemp(double celsius, String unit) {
        double val = convertTemp(celsius, unit);
        return String.format(java.util.Locale.US, "%.1f %s", val, getUnitSymbol(unit));
    }

    // Solar PV Calculation Model
    // 5 kWp standard residential rooftop setup (~25m² panels, 20% efficiency)
    public static double calcPvPower(double radiationWm2, double tempC) {
        if (radiationWm2 <= 5.0) return 0.0;
        double kwp = 5.0;
        double rawKw = (radiationWm2 / 1000.0) * kwp;
        double cellTemp = tempC + (radiationWm2 / 800.0) * 25.0;
        double tempFactor = cellTemp > 25.0 ? Math.max(0.7, 1.0 - 0.004 * (cellTemp - 25.0)) : 1.0;
        double performanceRatio = 0.85;
        double power = rawKw * tempFactor * performanceRatio;
        return Math.min(5.5, Math.max(0.0, power));
    }

    // JSON Serialization for SharedPreferences caching
    public String toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("locationName", locationName);
            obj.put("latitude", latitude);
            obj.put("longitude", longitude);
            obj.put("timestamp", timestamp);

            obj.put("currentTemp", currentTemp);
            obj.put("apparentTemp", apparentTemp);
            obj.put("relativeHumidity", relativeHumidity);
            obj.put("precipitation", precipitation);
            obj.put("weatherCode", weatherCode);
            obj.put("windSpeed", windSpeed);
            obj.put("windDirection", windDirection);
            obj.put("surfacePressure", surfacePressure);
            obj.put("cloudCover", cloudCover);
            obj.put("isDay", isDay);

            obj.put("pvPowerKw", pvPowerKw);
            obj.put("pvEnergyKwhToday", pvEnergyKwhToday);

            JSONArray hArr = new JSONArray();
            for (HourlyItem h : hourly) {
                JSONObject hObj = new JSONObject();
                hObj.put("time", h.time);
                hObj.put("temp", h.temp);
                hObj.put("weatherCode", h.weatherCode);
                hObj.put("rainProb", h.rainProb);
                hObj.put("radiation", h.radiation);
                hArr.put(hObj);
            }
            obj.put("hourly", hArr);

            JSONArray dArr = new JSONArray();
            for (DailyItem d : daily) {
                JSONObject dObj = new JSONObject();
                dObj.put("date", d.date);
                dObj.put("weatherCode", d.weatherCode);
                dObj.put("tempMax", d.tempMax);
                dObj.put("tempMin", d.tempMin);
                dObj.put("sunrise", d.sunrise);
                dObj.put("sunset", d.sunset);
                dObj.put("precipSum", d.precipSum);
                dArr.put(dObj);
            }
            obj.put("daily", dArr);

            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static WeatherData fromJson(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            WeatherData d = new WeatherData();
            d.locationName = obj.optString("locationName", "Siegen, NRW");
            d.latitude = obj.optDouble("latitude", 50.87);
            d.longitude = obj.optDouble("longitude", 8.02);
            d.timestamp = obj.optLong("timestamp", System.currentTimeMillis());

            d.currentTemp = obj.optDouble("currentTemp", 18.0);
            d.apparentTemp = obj.optDouble("apparentTemp", 17.5);
            d.relativeHumidity = obj.optInt("relativeHumidity", 65);
            d.precipitation = obj.optDouble("precipitation", 0.0);
            d.weatherCode = obj.optInt("weatherCode", 1);
            d.windSpeed = obj.optDouble("windSpeed", 12.0);
            d.windDirection = obj.optInt("windDirection", 220);
            d.surfacePressure = obj.optDouble("surfacePressure", 1014.0);
            d.cloudCover = obj.optInt("cloudCover", 25);
            d.isDay = obj.optBoolean("isDay", true);

            d.pvPowerKw = obj.optDouble("pvPowerKw", 2.8);
            d.pvEnergyKwhToday = obj.optDouble("pvEnergyKwhToday", 14.5);

            JSONArray hArr = obj.optJSONArray("hourly");
            if (hArr != null) {
                for (int i = 0; i < hArr.length(); i++) {
                    JSONObject hObj = hArr.getJSONObject(i);
                    HourlyItem hi = new HourlyItem();
                    hi.time = hObj.optString("time");
                    hi.temp = hObj.optDouble("temp");
                    hi.weatherCode = hObj.optInt("weatherCode");
                    hi.rainProb = hObj.optInt("rainProb");
                    hi.radiation = hObj.optDouble("radiation");
                    d.hourly.add(hi);
                }
            }

            JSONArray dArr = obj.optJSONArray("daily");
            if (dArr != null) {
                for (int i = 0; i < dArr.length(); i++) {
                    JSONObject dObj = dArr.getJSONObject(i);
                    DailyItem di = new DailyItem();
                    di.date = dObj.optString("date");
                    di.weatherCode = dObj.optInt("weatherCode");
                    di.tempMax = dObj.optDouble("tempMax");
                    di.tempMin = dObj.optDouble("tempMin");
                    di.sunrise = dObj.optString("sunrise");
                    di.sunset = dObj.optString("sunset");
                    di.precipSum = dObj.optDouble("precipSum");
                    d.daily.add(di);
                }
            }

            return d;
        } catch (Exception e) {
            return null;
        }
    }
}
