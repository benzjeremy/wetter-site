package com.benzjeremy.wetter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.benzjeremy.wetter.ACTION_REFRESH";
    private static final int PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | 0x04000000; // FLAG_IMMUTABLE

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
        }
        int interval = WeatherRepository.getAutoRefreshInterval(context);
        if (interval > 0) {
            WeatherRepository.scheduleAutoRefresh(context, interval);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        WeatherRepository.LocationInfo loc = WeatherRepository.loadLocation(context);
        WeatherData cache = WeatherRepository.loadCache(context);
        String unit = WeatherRepository.getUnit(context);

        // Click on widget opens main app
        Intent openAppIntent = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0, openAppIntent, PI_FLAGS));

        // Click on refresh triggers sync broadcast
        Intent refreshIntent = new Intent(context, WeatherWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        views.setOnClickPendingIntent(R.id.widget_refresh_btn, PendingIntent.getBroadcast(context, 0, refreshIntent, PI_FLAGS));

        if (cache != null) {
            views.setTextViewText(R.id.widget_location, cache.locationName != null ? cache.locationName : loc.name);
            views.setTextViewText(R.id.widget_temp, String.format(Locale.US, "%.0f°", WeatherData.convertTemp(cache.currentTemp, unit)));
            views.setTextViewText(R.id.widget_icon, WeatherData.getWeatherIcon(cache.weatherCode));
            views.setTextViewText(R.id.widget_desc, WeatherData.getWeatherDescription(cache.weatherCode, true));

            if (!cache.daily.isEmpty()) {
                WeatherData.DailyItem today = cache.daily.get(0);
                views.setTextViewText(R.id.widget_high_low, String.format(Locale.US, "▲ %.0f° · ▼ %.0f°",
                        WeatherData.convertTemp(today.tempMax, unit),
                        WeatherData.convertTemp(today.tempMin, unit)));
            } else {
                views.setTextViewText(R.id.widget_high_low, "▲ --° · ▼ --°");
            }

            // 3 Telemetry Pills (Matching WetterStationPreview.astro)
            views.setTextViewText(R.id.widget_pv, String.format(Locale.US, "⚡ PV: %.1f kW", cache.pvPowerKw));
            int rainProb = (!cache.hourly.isEmpty()) ? cache.hourly.get(0).rainProb : cache.relativeHumidity;
            views.setTextViewText(R.id.widget_rain, String.format(Locale.US, "💧 %d%%", rainProb));
            views.setTextViewText(R.id.widget_wind, String.format(Locale.US, "💨 %.0f km/h", cache.windSpeed));

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            views.setTextViewText(R.id.widget_updated, sdf.format(new Date(cache.timestamp)));
        } else {
            views.setTextViewText(R.id.widget_location, loc.name);
            views.setTextViewText(R.id.widget_temp, "--°");
            views.setTextViewText(R.id.widget_icon, "☀️");
            views.setTextViewText(R.id.widget_desc, "Tippe zum Laden");
            views.setTextViewText(R.id.widget_high_low, "▲ --° · ▼ --°");
            views.setTextViewText(R.id.widget_pv, "⚡ PV --");
            views.setTextViewText(R.id.widget_rain, "💧 --%");
            views.setTextViewText(R.id.widget_wind, "💨 -- km/h");
            views.setTextViewText(R.id.widget_updated, "");
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            int interval = WeatherRepository.getAutoRefreshInterval(context);
            if (interval > 0) {
                WeatherRepository.scheduleAutoRefresh(context, interval);
            }
            return;
        }

        if (ACTION_REFRESH.equals(intent.getAction())) {
            WeatherRepository.LocationInfo loc = WeatherRepository.loadLocation(context);
            WeatherRepository.fetchWeather(context, loc.latitude, loc.longitude, loc.name, new WidgetUpdateCallback(context));
        }
    }

    private static class WidgetUpdateCallback implements WeatherRepository.WeatherCallback {
        private final Context context;

        public WidgetUpdateCallback(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public void onSuccess(WeatherData data) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, WeatherWidgetProvider.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            for (int id : ids) {
                WeatherWidgetProvider.updateAppWidget(context, mgr, id);
            }
        }

        @Override
        public void onError(Exception e) {
            // Keep existing cache on widget if network fails
        }
    }
}
