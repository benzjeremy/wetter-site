package com.benzjeremy.wetter;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvLocationName;
    private Button btnUnitToggle;
    private ImageButton btnSearch;
    private ImageButton btnSettings;
    private ImageButton btnRefresh;

    private LinearLayout searchBarLayout;
    private EditText etSearchQuery;
    private Button btnSearchSubmit;
    private LinearLayout searchResultsContainer;

    private TextView tvStatusBanner;

    // Hero
    private TextView tvHeroIcon;
    private TextView tvHeroTemp;
    private TextView tvHeroCondition;
    private TextView tvHeroApparent;
    private TextView tvHeroMinMax;

    // Metrics
    private TextView tvMetricHumidity;
    private TextView tvMetricWind;
    private TextView tvMetricPressure;
    private TextView tvMetricCloud;

    // Solar PV
    private TextView tvPvCurrent;
    private TextView tvPvEnergy;

    // Containers
    private LinearLayout hourlyForecastContainer;
    private LinearLayout dailyForecastContainer;

    private String currentUnit = "celsius";
    private WeatherRepository.LocationInfo currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUnit = WeatherRepository.getUnit(this);
        currentLocation = WeatherRepository.loadLocation(this);

        initViews();
        setupListeners();

        // 1. Instant display from cache (offline-first, zero layout shift)
        WeatherData cached = WeatherRepository.loadCache(this);
        if (cached != null) {
            renderWeather(cached);
        } else {
            showLoading(true);
        }

        // 2. Fetch fresh weather from Open-Meteo API
        refreshWeather();
    }

    private void initViews() {
        tvLocationName = findViewById(R.id.tv_location_name);
        btnUnitToggle = findViewById(R.id.btn_unit_toggle);
        btnSearch = findViewById(R.id.btn_search);
        btnSettings = findViewById(R.id.btn_settings);
        btnRefresh = findViewById(R.id.btn_refresh);

        searchBarLayout = findViewById(R.id.search_bar_layout);
        etSearchQuery = findViewById(R.id.et_search_query);
        btnSearchSubmit = findViewById(R.id.btn_search_submit);
        searchResultsContainer = findViewById(R.id.search_results_container);

        tvStatusBanner = findViewById(R.id.tv_status_banner);

        tvHeroIcon = findViewById(R.id.tv_hero_icon);
        tvHeroTemp = findViewById(R.id.tv_hero_temp);
        tvHeroCondition = findViewById(R.id.tv_hero_condition);
        tvHeroApparent = findViewById(R.id.tv_hero_apparent);
        tvHeroMinMax = findViewById(R.id.tv_hero_minmax);

        tvMetricHumidity = findViewById(R.id.tv_metric_humidity);
        tvMetricWind = findViewById(R.id.tv_metric_wind);
        tvMetricPressure = findViewById(R.id.tv_metric_pressure);
        tvMetricCloud = findViewById(R.id.tv_metric_cloud);

        tvPvCurrent = findViewById(R.id.tv_pv_current);
        tvPvEnergy = findViewById(R.id.tv_pv_energy);

        hourlyForecastContainer = findViewById(R.id.hourly_forecast_container);
        dailyForecastContainer = findViewById(R.id.daily_forecast_container);

        updateUnitButtonLabel();
    }

    private void updateUnitButtonLabel() {
        btnUnitToggle.setText(WeatherData.getUnitSymbol(currentUnit));
    }

    private void setupListeners() {
        // Toggle search bar
        btnSearch.setOnClickListener(v -> {
            if (searchBarLayout.getVisibility() == View.VISIBLE) {
                searchBarLayout.setVisibility(View.GONE);
                hideKeyboard();
            } else {
                searchBarLayout.setVisibility(View.VISIBLE);
                etSearchQuery.requestFocus();
                showKeyboard();
            }
        });

        // Search submit
        btnSearchSubmit.setOnClickListener(v -> performSearch());
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // Unit toggle (Celsius -> Fahrenheit -> Kelvin)
        btnUnitToggle.setOnClickListener(v -> {
            if ("celsius".equalsIgnoreCase(currentUnit)) {
                currentUnit = "fahrenheit";
            } else if ("fahrenheit".equalsIgnoreCase(currentUnit)) {
                currentUnit = "kelvin";
            } else {
                currentUnit = "celsius";
            }
            WeatherRepository.setUnit(this, currentUnit);
            updateUnitButtonLabel();

            // Re-render
            WeatherData cached = WeatherRepository.loadCache(this);
            if (cached != null) {
                renderWeather(cached);
                syncWidget();
            }
        });

        // Manual Refresh
        btnRefresh.setOnClickListener(v -> refreshWeather());

        // Auto-refresh interval and legal settings
        btnSettings.setOnClickListener(v -> showSettingsMenuDialog());

        // Dedicated legal & privacy button
        Button btnLegal = findViewById(R.id.btn_legal_info);
        if (btnLegal != null) {
            btnLegal.setOnClickListener(v -> showLegalDialog());
        }

        // Location header click also triggers search
        findViewById(R.id.btn_location_header).setOnClickListener(v -> {
            searchBarLayout.setVisibility(View.VISIBLE);
            etSearchQuery.requestFocus();
            showKeyboard();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WeatherData cached = WeatherRepository.loadCache(this);
        if (cached != null) {
            renderWeather(cached);
        }
    }

    private void showSettingsMenuDialog() {
        String[] menuOptions = new String[]{
                "⏱️ " + getString(R.string.auto_refresh),
                getString(R.string.legal_and_privacy)
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.settings)
                .setItems(menuOptions, (dialog, which) -> {
                    if (which == 0) {
                        showAutoRefreshDialog();
                    } else if (which == 1) {
                        showLegalDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAutoRefreshDialog() {
        final int[] intervals = new int[]{15, 30, 60, 0};
        final String[] options = new String[]{
                getString(R.string.refresh_15m),
                getString(R.string.refresh_30m),
                getString(R.string.refresh_60m),
                getString(R.string.refresh_off)
        };

        int currentInterval = WeatherRepository.getAutoRefreshInterval(this);
        int selectedIndex = 2; // Default: 60m
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == currentInterval) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.auto_refresh)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    int chosenInterval = intervals[which];
                    WeatherRepository.setAutoRefreshInterval(MainActivity.this, chosenInterval);
                    WeatherRepository.scheduleAutoRefresh(MainActivity.this, chosenInterval);
                    Toast.makeText(MainActivity.this, getString(R.string.auto_refresh_set) + options[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLegalDialog() {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(18), dpToPx(14), dpToPx(18), dpToPx(14));

        addLegalSection(layout, getString(R.string.legal_impressum_heading), getString(R.string.legal_impressum_content));
        addLegalSection(layout, getString(R.string.legal_privacy_heading), getString(R.string.legal_privacy_content));
        addLegalSection(layout, getString(R.string.legal_contact_heading), getString(R.string.legal_contact_content));
        addLegalSection(layout, getString(R.string.legal_license_heading), getString(R.string.legal_license_content));

        sv.addView(layout);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.legal_dialog_title)
                .setView(sv)
                .setPositiveButton("Schließen", null)
                .show();
    }

    private void addLegalSection(LinearLayout container, String title, String content) {
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.brand_blue));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, dpToPx(8), 0, dpToPx(4));
        container.addView(tvTitle);

        TextView tvContent = new TextView(this);
        tvContent.setText(content);
        tvContent.setTextColor(getColor(R.color.text_primary));
        tvContent.setTextSize(12);
        tvContent.setLineSpacing(dpToPx(2), 1.15f);
        tvContent.setPadding(0, 0, 0, dpToPx(12));
        container.addView(tvContent);
    }

    private void refreshWeather() {
        showLoading(true);
        WeatherRepository.fetchWeather(this, currentLocation.latitude, currentLocation.longitude, currentLocation.name, new FetchCallback(this));
    }

    private static class FetchCallback implements WeatherRepository.WeatherCallback {
        private final MainActivity act;

        public FetchCallback(MainActivity act) {
            this.act = act;
        }

        @Override
        public void onSuccess(WeatherData data) {
            act.showLoading(false);
            act.renderWeather(data);
            act.syncWidget();
        }

        @Override
        public void onError(Exception e) {
            act.showLoading(false);
            act.tvStatusBanner.setVisibility(View.VISIBLE);
            act.tvStatusBanner.setText(act.getString(R.string.error_network));
            Toast.makeText(act, R.string.error_network, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean loading) {
        if (loading) {
            tvStatusBanner.setVisibility(View.VISIBLE);
            tvStatusBanner.setText(getString(R.string.loading));
        } else {
            tvStatusBanner.setVisibility(View.GONE);
        }
    }

    private void renderWeather(WeatherData data) {
        tvLocationName.setText(data.locationName != null ? data.locationName : currentLocation.name);

        // Hero
        tvHeroIcon.setText(WeatherData.getWeatherIcon(data.weatherCode));
        tvHeroTemp.setText(WeatherData.formatTemp(data.currentTemp, currentUnit));
        tvHeroCondition.setText(WeatherData.getWeatherDescription(data.weatherCode, true));
        tvHeroApparent.setText(getString(R.string.apparent_temp) + ": " + WeatherData.formatTemp(data.apparentTemp, currentUnit));

        if (!data.daily.isEmpty()) {
            WeatherData.DailyItem today = data.daily.get(0);
            String high = String.format(Locale.US, "▲ %.0f°", WeatherData.convertTemp(today.tempMax, currentUnit));
            String low = String.format(Locale.US, "▼ %.0f°", WeatherData.convertTemp(today.tempMin, currentUnit));
            tvHeroMinMax.setText(high + "  " + low);
        }

        // Metrics
        tvMetricHumidity.setText(data.relativeHumidity + " %");
        tvMetricWind.setText(String.format(Locale.US, "%.1f km/h", data.windSpeed));
        tvMetricPressure.setText(String.format(Locale.US, "%.0f hPa", data.surfacePressure));
        tvMetricCloud.setText(data.cloudCover + " %");

        // Solar PV
        tvPvCurrent.setText(String.format(Locale.US, "%.2f kW", data.pvPowerKw));
        tvPvEnergy.setText(String.format(Locale.US, "%.1f kWh", data.pvEnergyKwhToday));

        // Hourly
        renderHourly(data.hourly);

        // Daily
        renderDaily(data.daily);
    }

    private void renderHourly(List<WeatherData.HourlyItem> hourly) {
        hourlyForecastContainer.removeAllViews();
        if (hourly == null) return;

        for (WeatherData.HourlyItem item : hourly) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
            box.setBackgroundResource(R.drawable.btn_bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dpToPx(8), 0);
            box.setLayoutParams(lp);

            // Time
            TextView tvTime = new TextView(this);
            tvTime.setText(item.time);
            tvTime.setTextSize(11);
            tvTime.setTextColor(getColor(R.color.text_muted));
            box.addView(tvTime);

            // Icon
            TextView tvIcon = new TextView(this);
            tvIcon.setText(WeatherData.getWeatherIcon(item.weatherCode));
            tvIcon.setTextSize(20);
            tvIcon.setPadding(0, dpToPx(4), 0, dpToPx(4));
            box.addView(tvIcon);

            // Temp
            TextView tvTemp = new TextView(this);
            tvTemp.setText(String.format(Locale.US, "%.0f°", WeatherData.convertTemp(item.temp, currentUnit)));
            tvTemp.setTextSize(13);
            tvTemp.setTypeface(null, Typeface.BOLD);
            tvTemp.setTextColor(getColor(R.color.text_primary));
            box.addView(tvTemp);

            // Rain Prob
            if (item.rainProb > 0) {
                TextView tvRain = new TextView(this);
                tvRain.setText(item.rainProb + "%");
                tvRain.setTextSize(10);
                tvRain.setTextColor(getColor(R.color.brand_blue));
                box.addView(tvRain);
            }

            hourlyForecastContainer.addView(box);
        }
    }

    private void renderDaily(List<WeatherData.DailyItem> daily) {
        dailyForecastContainer.removeAllViews();
        if (daily == null) return;

        for (int i = 0; i < daily.size(); i++) {
            WeatherData.DailyItem item = daily.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(8), 0, dpToPx(8));

            // Date / Day Label
            TextView tvDay = new TextView(this);
            tvDay.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvDay.setText(i == 0 ? "Heute" : item.date);
            tvDay.setTextSize(13);
            tvDay.setTypeface(null, Typeface.BOLD);
            tvDay.setTextColor(getColor(R.color.text_primary));
            row.addView(tvDay);

            // Icon
            TextView tvIcon = new TextView(this);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvIcon.setText(WeatherData.getWeatherIcon(item.weatherCode));
            tvIcon.setTextSize(20);
            row.addView(tvIcon);

            // Description
            TextView tvDesc = new TextView(this);
            LinearLayout.LayoutParams lpDesc = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvDesc.setLayoutParams(lpDesc);
            tvDesc.setText(WeatherData.getWeatherDescription(item.weatherCode, true));
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(getColor(R.color.text_secondary));
            row.addView(tvDesc);

            // Min / Max
            TextView tvMinMax = new TextView(this);
            String minMax = String.format(Locale.US, "%.0f° / %.0f°",
                    WeatherData.convertTemp(item.tempMin, currentUnit),
                    WeatherData.convertTemp(item.tempMax, currentUnit));
            tvMinMax.setText(minMax);
            tvMinMax.setTextSize(13);
            tvMinMax.setTypeface(null, Typeface.BOLD);
            tvMinMax.setTextColor(getColor(R.color.brand_blue));
            row.addView(tvMinMax);

            dailyForecastContainer.addView(row);

            if (i < daily.size() - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
                divider.setBackgroundColor(getColor(R.color.border_subtle));
                dailyForecastContainer.addView(divider);
            }
        }
    }

    private void performSearch() {
        String q = etSearchQuery.getText().toString().trim();
        if (q.isEmpty()) return;

        searchResultsContainer.removeAllViews();
        TextView tvSearching = new TextView(this);
        tvSearching.setText("Suche läuft...");
        tvSearching.setTextColor(getColor(R.color.text_muted));
        tvSearching.setTextSize(12);
        searchResultsContainer.addView(tvSearching);

        WeatherRepository.searchCities(q, new CitySearchCallback(this));
    }

    private static class CitySearchCallback implements WeatherRepository.SearchCallback {
        private final MainActivity act;

        public CitySearchCallback(MainActivity act) {
            this.act = act;
        }

        @Override
        public void onSuccess(List<WeatherRepository.CityResult> results) {
            act.searchResultsContainer.removeAllViews();
            if (results.isEmpty()) {
                TextView tvNone = new TextView(act);
                tvNone.setText(act.getString(R.string.no_results));
                tvNone.setTextColor(act.getColor(R.color.text_muted));
                tvNone.setTextSize(12);
                act.searchResultsContainer.addView(tvNone);
                return;
            }

            for (WeatherRepository.CityResult cr : results) {
                Button btn = new Button(act);
                String label = cr.name + (cr.admin1 != null && !cr.admin1.isEmpty() ? ", " + cr.admin1 : "") + (cr.country != null ? " (" + cr.country + ")" : "");
                btn.setText(label);
                btn.setTextSize(12);
                btn.setTextColor(act.getColor(R.color.text_primary));
                btn.setBackgroundResource(R.drawable.btn_bg);
                btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                btn.setPadding(act.dpToPx(12), act.dpToPx(8), act.dpToPx(12), act.dpToPx(8));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, act.dpToPx(4), 0, 0);
                btn.setLayoutParams(lp);

                btn.setOnClickListener(v -> {
                    act.currentLocation = new WeatherRepository.LocationInfo(cr.name, cr.latitude, cr.longitude);
                    WeatherRepository.saveLocation(act, cr.name, cr.latitude, cr.longitude);
                    act.searchBarLayout.setVisibility(View.GONE);
                    act.hideKeyboard();
                    act.refreshWeather();
                });

                act.searchResultsContainer.addView(btn);
            }
        }

        @Override
        public void onError(Exception e) {
            act.searchResultsContainer.removeAllViews();
            TextView tvErr = new TextView(act);
            tvErr.setText("Fehler bei der Ortssuche.");
            tvErr.setTextColor(act.getColor(R.color.brand_red));
            tvErr.setTextSize(12);
            act.searchResultsContainer.addView(tvErr);
        }
    }

    private void syncWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, WeatherWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int id : appWidgetIds) {
            WeatherWidgetProvider.updateAppWidget(this, appWidgetManager, id);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
