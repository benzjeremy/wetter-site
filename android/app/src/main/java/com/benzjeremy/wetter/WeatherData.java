package com.benzjeremy.wetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class WeatherData {
    public double apparentTemp;
    public int cloudCover;
    public double currentTemp;
    public boolean isDay;
    public double latitude;
    public String locationName;
    public double longitude;
    public double precipitation;
    public double pvEnergyKwhToday;
    public double pvPowerKw;
    public int relativeHumidity;
    public double surfacePressure;
    public long timestamp;
    public int weatherCode;
    public int windDirection;
    public double windSpeed;
    public List<HourlyItem> hourly = new ArrayList<>();
    public List<DailyItem> daily = new ArrayList<>();

    /* loaded from: classes.dex */
    public static class DailyItem {
        public String date;
        public double precipSum;
        public String sunrise;
        public String sunset;
        public double tempMax;
        public double tempMin;
        public int weatherCode;
    }

    /* loaded from: classes.dex */
    public static class HourlyItem {
        public double radiation;
        public int rainProb;
        public double temp;
        public String time;
        public int weatherCode;
    }

    public static String getWeatherDescription(int i, boolean z) {
        switch (i) {
            case 0:
                return z ? "Klarer Himmel" : "Clear sky";
            case 1:
                return z ? "Überwiegend klar" : "Mainly clear";
            case 2:
                return z ? "Teilweise bewölkt" : "Partly cloudy";
            case 3:
                return z ? "Bedeckt" : "Overcast";
            case 45:
                return z ? "Nebel" : "Fog";
            case 48:
                return z ? "Reifnebel" : "Depositing rime fog";
            case 51:
                return z ? "Leichter Nieselregen" : "Light drizzle";
            case 53:
                return z ? "Mäßiger Nieselregen" : "Moderate drizzle";
            case 55:
                return z ? "Dichter Nieselregen" : "Dense drizzle";
            case 56:
            case 57:
                return z ? "Gefrierender Nieselregen" : "Freezing drizzle";
            case 61:
                return z ? "Leichter Regen" : "Slight rain";
            case 63:
                return z ? "Mäßiger Regen" : "Moderate rain";
            case 65:
                return z ? "Starker Regen" : "Heavy rain";
            case 66:
            case 67:
                return z ? "Gefrierender Regen" : "Freezing rain";
            case 71:
                return z ? "Leichter Schneefall" : "Slight snow fall";
            case 73:
                return z ? "Mäßiger Schneefall" : "Moderate snow fall";
            case 75:
                return z ? "Starker Schneefall" : "Heavy snow fall";
            case 77:
                return z ? "Schneegriesel" : "Snow grains";
            case 80:
                return z ? "Leichte Regenschauer" : "Slight rain showers";
            case 81:
                return z ? "Mäßige Regenschauer" : "Moderate rain showers";
            case 82:
                return z ? "Heftige Regenschauer" : "Violent rain showers";
            case 85:
            case 86:
                return z ? "Schneeschauer" : "Snow showers";
            case 95:
                return z ? "Gewitter" : "Thunderstorm";
            case 96:
            case 99:
                return z ? "Gewitter mit Hagel" : "Thunderstorm with hail";
            default:
                return z ? "Leicht bewölkt" : "Partly cloudy";
        }
    }

    public static String getWeatherIcon(int i) {
        switch (i) {
            case 0:
                return "☀️";
            case 1:
                return "🌤️";
            case 2:
                return "⛅";
            case 3:
                return "☁️";
            case 45:
            case 48:
                return "🌫️";
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
                return "🌦️";
            case 61:
            case 63:
            case 65:
            case 80:
            case 81:
            case 82:
                return "🌧️";
            case 66:
            case 67:
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return "❄️";
            case 95:
            case 96:
            case 99:
                return "⛈️";
            default:
                return "⛅";
        }
    }

    public static double convertTemp(double d, String str) {
        if ("fahrenheit".equalsIgnoreCase(str)) {
            return ((d * 9.0d) / 5.0d) + 32.0d;
        }
        if ("kelvin".equalsIgnoreCase(str)) {
            return d + 273.15d;
        }
        return d;
    }

    public static String getUnitSymbol(String str) {
        return "fahrenheit".equalsIgnoreCase(str) ? "°F" : "kelvin".equalsIgnoreCase(str) ? "K" : "°C";
    }

    public static String formatTemp(double d, String str) {
        return String.format(Locale.US, "%.1f %s", Double.valueOf(convertTemp(d, str)), getUnitSymbol(str));
    }

    public static double calcPvPower(double d, double d2) {
        if (d <= 5.0d) {
            return 0.0d;
        }
        double d3 = d2 + ((d / 800.0d) * 25.0d);
        return Math.min(5.5d, Math.max(0.0d, (d / 1000.0d) * 5.0d * (d3 > 25.0d ? Math.max(0.7d, 1.0d - ((d3 - 25.0d) * 0.004d)) : 1.0d) * 0.85d));
    }

    public String toJson() {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("locationName", this.locationName);
            jSONObject.put("latitude", this.latitude);
            jSONObject.put("longitude", this.longitude);
            jSONObject.put("timestamp", this.timestamp);
            jSONObject.put("currentTemp", this.currentTemp);
            jSONObject.put("apparentTemp", this.apparentTemp);
            jSONObject.put("relativeHumidity", this.relativeHumidity);
            jSONObject.put("precipitation", this.precipitation);
            jSONObject.put("weatherCode", this.weatherCode);
            jSONObject.put("windSpeed", this.windSpeed);
            jSONObject.put("windDirection", this.windDirection);
            jSONObject.put("surfacePressure", this.surfacePressure);
            jSONObject.put("cloudCover", this.cloudCover);
            jSONObject.put("isDay", this.isDay);
            jSONObject.put("pvPowerKw", this.pvPowerKw);
            jSONObject.put("pvEnergyKwhToday", this.pvEnergyKwhToday);
            JSONArray jSONArray = new JSONArray();
            for (HourlyItem hourlyItem : this.hourly) {
                JSONObject jSONObject2 = new JSONObject();
                jSONObject2.put("time", hourlyItem.time);
                jSONObject2.put("temp", hourlyItem.temp);
                jSONObject2.put("weatherCode", hourlyItem.weatherCode);
                jSONObject2.put("rainProb", hourlyItem.rainProb);
                jSONObject2.put("radiation", hourlyItem.radiation);
                jSONArray.put(jSONObject2);
            }
            jSONObject.put("hourly", jSONArray);
            JSONArray jSONArray2 = new JSONArray();
            for (DailyItem dailyItem : this.daily) {
                JSONObject jSONObject3 = new JSONObject();
                jSONObject3.put("date", dailyItem.date);
                jSONObject3.put("weatherCode", dailyItem.weatherCode);
                jSONObject3.put("tempMax", dailyItem.tempMax);
                jSONObject3.put("tempMin", dailyItem.tempMin);
                jSONObject3.put("sunrise", dailyItem.sunrise);
                jSONObject3.put("sunset", dailyItem.sunset);
                jSONObject3.put("precipSum", dailyItem.precipSum);
                jSONArray2.put(jSONObject3);
            }
            jSONObject.put("daily", jSONArray2);
            return jSONObject.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static WeatherData fromJson(String str) {
        try {
            JSONObject jSONObject = new JSONObject(str);
            WeatherData weatherData = new WeatherData();
            weatherData.locationName = jSONObject.optString("locationName", "Siegen, NRW");
            weatherData.latitude = jSONObject.optDouble("latitude", 50.87d);
            weatherData.longitude = jSONObject.optDouble("longitude", 8.02d);
            weatherData.timestamp = jSONObject.optLong("timestamp", System.currentTimeMillis());
            weatherData.currentTemp = jSONObject.optDouble("currentTemp", 18.0d);
            weatherData.apparentTemp = jSONObject.optDouble("apparentTemp", 17.5d);
            weatherData.relativeHumidity = jSONObject.optInt("relativeHumidity", 65);
            weatherData.precipitation = jSONObject.optDouble("precipitation", 0.0d);
            weatherData.weatherCode = jSONObject.optInt("weatherCode", 1);
            weatherData.windSpeed = jSONObject.optDouble("windSpeed", 12.0d);
            weatherData.windDirection = jSONObject.optInt("windDirection", 220);
            weatherData.surfacePressure = jSONObject.optDouble("surfacePressure", 1014.0d);
            weatherData.cloudCover = jSONObject.optInt("cloudCover", 25);
            weatherData.isDay = jSONObject.optBoolean("isDay", true);
            weatherData.pvPowerKw = jSONObject.optDouble("pvPowerKw", 2.8d);
            weatherData.pvEnergyKwhToday = jSONObject.optDouble("pvEnergyKwhToday", 14.5d);
            JSONArray optJSONArray = jSONObject.optJSONArray("hourly");
            if (optJSONArray != null) {
                for (int i = 0; i < optJSONArray.length(); i++) {
                    JSONObject jSONObject2 = optJSONArray.getJSONObject(i);
                    HourlyItem hourlyItem = new HourlyItem();
                    hourlyItem.time = jSONObject2.optString("time");
                    hourlyItem.temp = jSONObject2.optDouble("temp");
                    hourlyItem.weatherCode = jSONObject2.optInt("weatherCode");
                    hourlyItem.rainProb = jSONObject2.optInt("rainProb");
                    hourlyItem.radiation = jSONObject2.optDouble("radiation");
                    weatherData.hourly.add(hourlyItem);
                }
            }
            JSONArray optJSONArray2 = jSONObject.optJSONArray("daily");
            if (optJSONArray2 != null) {
                for (int i2 = 0; i2 < optJSONArray2.length(); i2++) {
                    JSONObject jSONObject3 = optJSONArray2.getJSONObject(i2);
                    DailyItem dailyItem = new DailyItem();
                    dailyItem.date = jSONObject3.optString("date");
                    dailyItem.weatherCode = jSONObject3.optInt("weatherCode");
                    dailyItem.tempMax = jSONObject3.optDouble("tempMax");
                    dailyItem.tempMin = jSONObject3.optDouble("tempMin");
                    dailyItem.sunrise = jSONObject3.optString("sunrise");
                    dailyItem.sunset = jSONObject3.optString("sunset");
                    dailyItem.precipSum = jSONObject3.optDouble("precipSum");
                    weatherData.daily.add(dailyItem);
                }
            }
            return weatherData;
        } catch (Exception e) {
            return null;
        }
    }
}
