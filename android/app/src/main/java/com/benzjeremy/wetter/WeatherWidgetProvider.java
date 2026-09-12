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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        WeatherRepository.LocationInfo loc = WeatherRepository.loadLocation(context);
        WeatherData data = WeatherRepository.loadCache(context);
        String unit = WeatherRepository.getUnit(context);

        // Click on widget body -> Open MainActivity
        Intent launchIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Click on refresh button -> Broadcast ACTION_REFRESH
        Intent refreshIntent = new Intent(context, WeatherWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent);

        // Fill Data
        if (data != null) {
            views.setTextViewText(R.id.widget_location, data.locationName != null ? data.locationName : loc.name);
            views.setTextViewText(R.id.widget_temp, WeatherData.formatTemp(data.currentTemp, unit));
            views.setTextViewText(R.id.widget_icon, WeatherData.getWeatherIcon(data.weatherCode));
            views.setTextViewText(R.id.widget_desc, WeatherData.getWeatherDescription(data.weatherCode, true));

            if (!data.daily.isEmpty()) {
                WeatherData.DailyItem today = data.daily.get(0);
                String highStr = String.format(Locale.US, "▲ %.0f°", WeatherData.convertTemp(today.tempMax, unit));
                String lowStr = String.format(Locale.US, "▼ %.0f°", WeatherData.convertTemp(today.tempMin, unit));
                views.setTextViewText(R.id.widget_high_low, highStr + "\n" + lowStr);
            }

            String pvStr = String.format(Locale.US, "☀️ PV: %.1f kW • Heute ~%.1f kWh", data.pvPowerKw, data.pvEnergyKwhToday);
            views.setTextViewText(R.id.widget_pv, pvStr);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            views.setTextViewText(R.id.widget_updated, sdf.format(new Date(data.timestamp)));
        } else {
            views.setTextViewText(R.id.widget_location, loc.name);
            views.setTextViewText(R.id.widget_temp, "-- °C");
            views.setTextViewText(R.id.widget_icon, "☀️");
            views.setTextViewText(R.id.widget_desc, "Tippe zum Laden");
            views.setTextViewText(R.id.widget_pv, "☀️ PV-Prognose bereit");
            views.setTextViewText(R.id.widget_updated, "");
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

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
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, WeatherWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int id : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id);
            }
        }

        @Override
        public void onError(Exception e) {
            // Cache remains intact, widget gracefully degrades
        }
    }
}
