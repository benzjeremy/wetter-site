package com.benzjeremy.wetter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements View.OnClickListener, TextView.OnEditorActionListener {
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

    private TextView tvHeroIcon;
    private TextView tvHeroTemp;
    private TextView tvHeroCondition;
    private TextView tvHeroApparent;
    private TextView tvHeroMinMax;

    private TextView tvMetricHumidity;
    private TextView tvMetricWind;
    private TextView tvMetricPressure;
    private TextView tvMetricCloud;

    private TextView tvPvCurrent;
    private TextView tvPvEnergy;

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

        WeatherData cached = WeatherRepository.loadCache(this);
        if (cached != null) {
            renderWeather(cached);
        } else {
            showLoading(true);
        }

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
        btnUnitToggle.setOnClickListener(this);
        btnRefresh.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnSettings.setOnClickListener(this);
        btnSearchSubmit.setOnClickListener(this);

        View locationHeader = findViewById(R.id.btn_location_header);
        if (locationHeader != null) {
            locationHeader.setOnClickListener(this);
        }

        etSearchQuery.setOnEditorActionListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_unit_toggle) {
            cycleUnit();
        } else if (id == R.id.btn_refresh) {
            refreshWeather();
        } else if (id == R.id.btn_search || id == R.id.btn_location_header) {
            toggleSearchBar();
        } else if (id == R.id.btn_settings) {
            showSettingsMenuDialog();
        } else if (id == R.id.btn_search_submit) {
            performSearch();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            performSearch();
            return true;
        }
        return false;
    }

    private void toggleSearchBar() {
        if (searchBarLayout.getVisibility() == View.VISIBLE) {
            searchBarLayout.setVisibility(View.GONE);
            hideKeyboard();
        } else {
            searchBarLayout.setVisibility(View.VISIBLE);
            etSearchQuery.requestFocus();
            showKeyboard();
        }
    }

    private void cycleUnit() {
        if ("celsius".equalsIgnoreCase(currentUnit)) {
            currentUnit = "fahrenheit";
        } else if ("fahrenheit".equalsIgnoreCase(currentUnit)) {
            currentUnit = "kelvin";
        } else {
            currentUnit = "celsius";
        }
        WeatherRepository.setUnit(this, currentUnit);
        updateUnitButtonLabel();

        WeatherData cached = WeatherRepository.loadCache(this);
        if (cached != null) {
            renderWeather(cached);
        }
        syncWidget();
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
        String[] options = new String[]{
                "⏱️ " + getString(R.string.auto_refresh),
                getString(R.string.legal_and_privacy)
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.settings)
                .setItems(options, new SettingsMenuClickListener(this))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static class SettingsMenuClickListener implements DialogInterface.OnClickListener {
        private final MainActivity act;

        public SettingsMenuClickListener(MainActivity act) {
            this.act = act;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == 0) {
                act.showAutoRefreshDialog();
            } else if (which == 1) {
                act.showLegalDialog();
            }
        }
    }

    private void showAutoRefreshDialog() {
        final int[] intervals = {15, 30, 60, 0};
        final String[] labels = {
                getString(R.string.refresh_15m),
                getString(R.string.refresh_30m),
                getString(R.string.refresh_60m),
                getString(R.string.refresh_off)
        };

        int current = WeatherRepository.getAutoRefreshInterval(this);
        int selectedIndex = 2;
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == current) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.auto_refresh)
                .setSingleChoiceItems(labels, selectedIndex, new AutoRefreshIntervalClickListener(this, intervals, labels))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static class AutoRefreshIntervalClickListener implements DialogInterface.OnClickListener {
        private final MainActivity act;
        private final int[] intervals;
        private final String[] labels;

        public AutoRefreshIntervalClickListener(MainActivity act, int[] intervals, String[] labels) {
            this.act = act;
            this.intervals = intervals;
            this.labels = labels;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int minutes = intervals[which];
            WeatherRepository.setAutoRefreshInterval(act, minutes);
            WeatherRepository.scheduleAutoRefresh(act, minutes);
            Toast.makeText(act, act.getString(R.string.auto_refresh_set) + labels[which], Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    private void showLegalDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(18), dpToPx(14), dpToPx(18), dpToPx(14));

        addLegalSection(content, getString(R.string.legal_impressum_heading), getString(R.string.legal_impressum_content));
        addLegalSection(content, getString(R.string.legal_privacy_heading), getString(R.string.legal_privacy_content));
        addLegalSection(content, getString(R.string.legal_contact_heading), getString(R.string.legal_contact_content));
        addLegalSection(content, getString(R.string.legal_license_heading), getString(R.string.legal_license_content));

        scrollView.addView(content);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.legal_dialog_title)
                .setView(scrollView)
                .setPositiveButton("Schließen", null)
                .show();
    }

    private void addLegalSection(LinearLayout container, String title, String body) {
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.brand_blue));
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, dpToPx(8), 0, dpToPx(4));
        container.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextColor(getColor(R.color.text_primary));
        tvBody.setTextSize(12f);
        tvBody.setLineSpacing(dpToPx(2), 1.15f);
        tvBody.setPadding(0, 0, 0, dpToPx(12));
        container.addView(tvBody);
    }

    private void refreshWeather() {
        showLoading(true);
        WeatherRepository.fetchWeather(this, currentLocation.latitude, currentLocation.longitude, currentLocation.name, new FetchCallback(this));
    }

    public static class FetchCallback implements WeatherRepository.WeatherCallback {
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

    private void showLoading(boolean show) {
        if (show) {
            tvStatusBanner.setVisibility(View.VISIBLE);
            tvStatusBanner.setText(getString(R.string.loading));
        } else {
            tvStatusBanner.setVisibility(View.GONE);
        }
    }

    private void renderWeather(WeatherData data) {
        String locDisplay = (data.locationName != null ? data.locationName : currentLocation.name) + " • Open-Meteo";
        tvLocationName.setText(locDisplay);

        tvHeroIcon.setText(WeatherData.getWeatherIcon(data.weatherCode));
        tvHeroTemp.setText(WeatherData.formatTemp(data.currentTemp, currentUnit));
        tvHeroCondition.setText(WeatherData.getWeatherDescription(data.weatherCode, true));
        tvHeroApparent.setText(getString(R.string.apparent_temp) + ": " + WeatherData.formatTemp(data.apparentTemp, currentUnit));

        if (!data.daily.isEmpty()) {
            WeatherData.DailyItem today = data.daily.get(0);
            tvHeroMinMax.setText(String.format(Locale.US, "▲ %.0f°  ▼ %.0f°",
                    WeatherData.convertTemp(today.tempMax, currentUnit),
                    WeatherData.convertTemp(today.tempMin, currentUnit)));
        }

        tvMetricHumidity.setText(data.relativeHumidity + " %");
        tvMetricWind.setText(String.format(Locale.US, "%.1f km/h", data.windSpeed));
        tvMetricPressure.setText(String.format(Locale.US, "%.0f hPa", data.surfacePressure));
        tvMetricCloud.setText(data.cloudCover + " %");

        tvPvCurrent.setText(String.format(Locale.US, "%.2f kW", data.pvPowerKw));
        tvPvEnergy.setText(String.format(Locale.US, "%.1f kWh", data.pvEnergyKwhToday));

        renderHourly(data.hourly);
        renderDaily(data.daily);
    }

    private void renderHourly(List<WeatherData.HourlyItem> hourly) {
        hourlyForecastContainer.removeAllViews();
        if (hourly == null) return;

        for (WeatherData.HourlyItem item : hourly) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(android.view.Gravity.CENTER);
            card.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
            card.setBackgroundResource(R.drawable.btn_bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dpToPx(8), 0);
            card.setLayoutParams(lp);

            TextView tvTime = new TextView(this);
            tvTime.setText(item.time);
            tvTime.setTextSize(11f);
            tvTime.setTextColor(getColor(R.color.text_muted));
            card.addView(tvTime);

            TextView tvIcon = new TextView(this);
            tvIcon.setText(WeatherData.getWeatherIcon(item.weatherCode));
            tvIcon.setTextSize(22f);
            tvIcon.setPadding(0, dpToPx(4), 0, dpToPx(4));
            card.addView(tvIcon);

            TextView tvTemp = new TextView(this);
            tvTemp.setText(String.format(Locale.US, "%.0f°", WeatherData.convertTemp(item.temp, currentUnit)));
            tvTemp.setTextSize(14f);
            tvTemp.setTypeface(null, Typeface.BOLD);
            tvTemp.setTextColor(getColor(R.color.text_primary));
            card.addView(tvTemp);

            if (item.rainProb > 0) {
                TextView tvRain = new TextView(this);
                tvRain.setText(item.rainProb + "%");
                tvRain.setTextSize(10f);
                tvRain.setTextColor(getColor(R.color.brand_blue));
                card.addView(tvRain);
            }

            hourlyForecastContainer.addView(card);
        }
    }

    private void renderDaily(List<WeatherData.DailyItem> daily) {
        dailyForecastContainer.removeAllViews();
        if (daily == null) return;

        for (int i = 0; i < daily.size(); i++) {
            WeatherData.DailyItem item = daily.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(10), 0, dpToPx(10));

            TextView tvDate = new TextView(this);
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvDate.setText(i == 0 ? "Heute" : item.date);
            tvDate.setTextSize(13f);
            tvDate.setTypeface(null, Typeface.BOLD);
            tvDate.setTextColor(getColor(R.color.text_primary));
            row.addView(tvDate);

            TextView tvIcon = new TextView(this);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvIcon.setText(WeatherData.getWeatherIcon(item.weatherCode));
            tvIcon.setTextSize(20f);
            row.addView(tvIcon);

            TextView tvDesc = new TextView(this);
            tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            tvDesc.setText(WeatherData.getWeatherDescription(item.weatherCode, true));
            tvDesc.setTextSize(13f);
            tvDesc.setTextColor(getColor(R.color.text_secondary));
            row.addView(tvDesc);

            TextView tvTemp = new TextView(this);
            tvTemp.setText(String.format(Locale.US, "%.0f° / %.0f°",
                    WeatherData.convertTemp(item.tempMin, currentUnit),
                    WeatherData.convertTemp(item.tempMax, currentUnit)));
            tvTemp.setTextSize(13f);
            tvTemp.setTypeface(null, Typeface.BOLD);
            tvTemp.setTextColor(getColor(R.color.brand_blue));
            row.addView(tvTemp);

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
        String query = etSearchQuery.getText().toString().trim();
        if (query.isEmpty()) return;

        searchResultsContainer.removeAllViews();
        TextView searching = new TextView(this);
        searching.setText("Suche läuft...");
        searching.setTextColor(getColor(R.color.text_muted));
        searching.setTextSize(12f);
        searchResultsContainer.addView(searching);

        WeatherRepository.searchCities(query, new CitySearchCallback(this));
    }

    public static class CitySearchCallback implements WeatherRepository.SearchCallback {
        private final MainActivity act;

        public CitySearchCallback(MainActivity act) {
            this.act = act;
        }

        @Override
        public void onSuccess(final List<WeatherRepository.CityResult> results) {
            act.searchResultsContainer.removeAllViews();
            if (results.isEmpty()) {
                TextView noResults = new TextView(act);
                noResults.setText(R.string.no_results);
                noResults.setTextColor(act.getColor(R.color.text_muted));
                noResults.setTextSize(13f);
                act.searchResultsContainer.addView(noResults);
                return;
            }

            for (final WeatherRepository.CityResult city : results) {
                LinearLayout item = new LinearLayout(act);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setGravity(android.view.Gravity.CENTER_VERTICAL);
                item.setPadding(act.dpToPx(10), act.dpToPx(8), act.dpToPx(10), act.dpToPx(8));
                item.setBackgroundResource(R.drawable.btn_bg);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, 0, 0, act.dpToPx(6));
                item.setLayoutParams(lp);

                TextView tvName = new TextView(act);
                String label = city.name + (city.admin1 != null && !city.admin1.isEmpty() ? ", " + city.admin1 : "") +
                        (city.country != null && !city.country.isEmpty() ? " (" + city.country + ")" : "");
                tvName.setText(label);
                tvName.setTextColor(act.getColor(R.color.text_primary));
                tvName.setTextSize(13f);
                item.addView(tvName);

                item.setOnClickListener(new CityItemClickListener(act, city));
                act.searchResultsContainer.addView(item);
            }
        }

        @Override
        public void onError(Exception e) {
            act.searchResultsContainer.removeAllViews();
            TextView errorView = new TextView(act);
            errorView.setText("Fehler bei der Ortssuche.");
            errorView.setTextColor(act.getColor(R.color.brand_red));
            errorView.setTextSize(13f);
            act.searchResultsContainer.addView(errorView);
        }
    }

    public static class CityItemClickListener implements View.OnClickListener {
        private final MainActivity act;
        private final WeatherRepository.CityResult city;

        public CityItemClickListener(MainActivity act, WeatherRepository.CityResult city) {
            this.act = act;
            this.city = city;
        }

        @Override
        public void onClick(View v) {
            String chosenName = city.name + (city.admin1 != null && !city.admin1.isEmpty() ? ", " + city.admin1 : "");
            act.currentLocation = new WeatherRepository.LocationInfo(chosenName, city.latitude, city.longitude);
            WeatherRepository.saveLocation(act, chosenName, city.latitude, city.longitude);

            act.searchBarLayout.setVisibility(View.GONE);
            act.etSearchQuery.setText("");
            act.searchResultsContainer.removeAllViews();
            act.hideKeyboard();

            act.refreshWeather();
        }
    }

    private void syncWidget() {
        Intent intent = new Intent(this, WeatherWidgetProvider.class);
        intent.setAction(WeatherWidgetProvider.ACTION_REFRESH);
        sendBroadcast(intent);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
