package com.benzjeremy.wetter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements View.OnClickListener, TextView.OnEditorActionListener {
    private TextView tvLocationName;
    private Button btnFavoriteToggle;
    private Button btnUnitToggle;
    private ImageButton btnSearch;
    private ImageButton btnSettings;
    private ImageButton btnRefresh;

    private LinearLayout favoritesBarLayout;
    private LinearLayout favoritesChipsContainer;

    private LinearLayout searchBarLayout;
    private EditText etSearchQuery;
    private Button btnSearchSubmit;
    private LinearLayout searchResultsContainer;
    private TextView tvStatusBanner;

    // Feature 2: Smart Weather Alerts
    private LinearLayout alertCardLayout;
    private TextView tvAlertIcon;
    private TextView tvAlertTitle;
    private TextView tvAlertDesc;
    private TextView tvAlertBadge;

    // Hero Card
    private TextView tvCardTimestamp;
    private TextView tvHeroIcon;
    private TextView tvHeroTemp;
    private TextView tvHeroCondition;
    private TextView tvHeroApparent;
    private TextView tvHeroPrecip;
    private TextView tvHeroMinMax;

    // Telemetry Grid
    private TextView tvMetricHumidity;
    private TextView tvMetricWind;
    private TextView tvMetricPressure;
    private TextView tvMetricCloud;

    // Solar PV Card
    private TextView tvPvCurrent;
    private TextView tvPvEnergy;

    // Feature 3: Astronomy & Sun Dashboard
    private TextView tvAstroSunrise;
    private TextView tvAstroSunset;
    private TextView tvAstroDaylight;
    private TextView tvAstroRadiation;

    // Forecast Containers
    private LinearLayout hourlyForecastContainer;
    private LinearLayout dailyForecastContainer;
    private TextView tvFooterLicense;

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
        renderFavoritesBar();
        updateFavoriteButton();

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
        btnFavoriteToggle = findViewById(R.id.btn_favorite_toggle);
        btnUnitToggle = findViewById(R.id.btn_unit_toggle);
        btnSearch = findViewById(R.id.btn_search);
        btnSettings = findViewById(R.id.btn_settings);
        btnRefresh = findViewById(R.id.btn_refresh);

        favoritesBarLayout = findViewById(R.id.favorites_bar_layout);
        favoritesChipsContainer = findViewById(R.id.favorites_chips_container);

        searchBarLayout = findViewById(R.id.search_bar_layout);
        etSearchQuery = findViewById(R.id.et_search_query);
        btnSearchSubmit = findViewById(R.id.btn_search_submit);
        searchResultsContainer = findViewById(R.id.search_results_container);
        tvStatusBanner = findViewById(R.id.tv_status_banner);

        alertCardLayout = findViewById(R.id.alert_card_layout);
        tvAlertIcon = findViewById(R.id.tv_alert_icon);
        tvAlertTitle = findViewById(R.id.tv_alert_title);
        tvAlertDesc = findViewById(R.id.tv_alert_desc);
        tvAlertBadge = findViewById(R.id.tv_alert_badge);

        tvCardTimestamp = findViewById(R.id.tv_card_timestamp);
        tvHeroIcon = findViewById(R.id.tv_hero_icon);
        tvHeroTemp = findViewById(R.id.tv_hero_temp);
        tvHeroCondition = findViewById(R.id.tv_hero_condition);
        tvHeroApparent = findViewById(R.id.tv_hero_apparent);
        tvHeroPrecip = findViewById(R.id.tv_hero_precip);
        tvHeroMinMax = findViewById(R.id.tv_hero_minmax);

        tvMetricHumidity = findViewById(R.id.tv_metric_humidity);
        tvMetricWind = findViewById(R.id.tv_metric_wind);
        tvMetricPressure = findViewById(R.id.tv_metric_pressure);
        tvMetricCloud = findViewById(R.id.tv_metric_cloud);

        tvPvCurrent = findViewById(R.id.tv_pv_current);
        tvPvEnergy = findViewById(R.id.tv_pv_energy);

        tvAstroSunrise = findViewById(R.id.tv_astro_sunrise);
        tvAstroSunset = findViewById(R.id.tv_astro_sunset);
        tvAstroDaylight = findViewById(R.id.tv_astro_daylight);
        tvAstroRadiation = findViewById(R.id.tv_astro_radiation);

        hourlyForecastContainer = findViewById(R.id.hourly_forecast_container);
        dailyForecastContainer = findViewById(R.id.daily_forecast_container);
        tvFooterLicense = findViewById(R.id.tv_footer_license);

        updateUnitButtonLabel();
    }

    private void updateUnitButtonLabel() {
        btnUnitToggle.setText(WeatherData.getUnitSymbol(currentUnit));
    }

    private void setupListeners() {
        btnFavoriteToggle.setOnClickListener(this);
        btnUnitToggle.setOnClickListener(this);
        btnRefresh.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnSettings.setOnClickListener(this);
        btnSearchSubmit.setOnClickListener(this);

        View locationHeader = findViewById(R.id.btn_location_header);
        if (locationHeader != null) {
            locationHeader.setOnClickListener(this);
        }

        if (tvFooterLicense != null) {
            tvFooterLicense.setOnClickListener(new FooterLicenseClickListener(this));
        }

        etSearchQuery.setOnEditorActionListener(this);

        setupQuickChip(R.id.chip_dortmund, "Dortmund, NRW", 51.5136, 7.4653);
        setupQuickChip(R.id.chip_siegen, "Siegen, NRW", 50.8748, 8.0243);
        setupQuickChip(R.id.chip_berlin, "Berlin", 52.5200, 13.4050);
        setupQuickChip(R.id.chip_muenchen, "München, Bayern", 48.1351, 11.5820);
        setupQuickChip(R.id.chip_hamburg, "Hamburg", 53.5511, 9.9937);
        setupQuickChip(R.id.chip_koeln, "Köln, NRW", 50.9375, 6.9603);
    }

    private void setupQuickChip(int resId, String name, double lat, double lon) {
        View view = findViewById(resId);
        if (view != null) {
            view.setOnClickListener(new QuickChipClickListener(this, name, lat, lon));
        }
    }

    public static class FooterLicenseClickListener implements View.OnClickListener {
        private final MainActivity act;

        public FooterLicenseClickListener(MainActivity act) {
            this.act = act;
        }

        @Override
        public void onClick(View v) {
            act.showLegalDialog();
        }
    }

    public static class QuickChipClickListener implements View.OnClickListener {
        private final MainActivity act;
        private final String name;
        private final double lat;
        private final double lon;

        public QuickChipClickListener(MainActivity act, String name, double lat, double lon) {
            this.act = act;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public void onClick(View v) {
            act.selectLocation(name, lat, lon);
        }
    }

    public void selectLocation(String name, double lat, double lon) {
        currentLocation = new WeatherRepository.LocationInfo(name, lat, lon);
        WeatherRepository.saveLocation(this, name, lat, lon);
        searchBarLayout.setVisibility(View.GONE);
        etSearchQuery.setText("");
        searchResultsContainer.removeAllViews();
        hideKeyboard();
        updateFavoriteButton();
        renderFavoritesBar();
        refreshWeather();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_favorite_toggle) {
            toggleFavoriteCurrentLocation();
        } else if (id == R.id.btn_unit_toggle) {
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

    private void toggleFavoriteCurrentLocation() {
        if (currentLocation == null) return;
        boolean isFav = WeatherRepository.isFavorite(this, currentLocation.name);
        if (isFav) {
            WeatherRepository.removeFavorite(this, currentLocation.name);
            Toast.makeText(this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
        } else {
            WeatherRepository.addFavorite(this, currentLocation.name, currentLocation.latitude, currentLocation.longitude);
            Toast.makeText(this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
        }
        updateFavoriteButton();
        renderFavoritesBar();
    }

    public void updateFavoriteButton() {
        if (currentLocation == null) return;
        boolean isFav = WeatherRepository.isFavorite(this, currentLocation.name);
        if (isFav) {
            btnFavoriteToggle.setText("★");
            btnFavoriteToggle.setTextColor(getColor(R.color.sun_gold));
        } else {
            btnFavoriteToggle.setText("☆");
            btnFavoriteToggle.setTextColor(getColor(R.color.text_secondary));
        }
    }

    public void renderFavoritesBar() {
        if (favoritesChipsContainer == null) return;
        favoritesChipsContainer.removeAllViews();

        List<WeatherRepository.FavoriteCity> favs = WeatherRepository.getFavorites(this);
        for (WeatherRepository.FavoriteCity fc : favs) {
            TextView chip = new TextView(this);
            chip.setText("★ " + fc.name);
            chip.setTextSize(12f);

            boolean isActive = currentLocation != null && (
                    fc.name.equalsIgnoreCase(currentLocation.name) ||
                            fc.name.toLowerCase().contains(currentLocation.name.toLowerCase()) ||
                            currentLocation.name.toLowerCase().contains(fc.name.toLowerCase())
            );

            if (isActive) {
                chip.setBackgroundResource(R.drawable.chip_bg_active);
                chip.setTextColor(getColor(R.color.brand_blue));
                chip.setTypeface(null, Typeface.BOLD);
            } else {
                chip.setBackgroundResource(R.drawable.chip_bg);
                chip.setTextColor(getColor(R.color.text_secondary));
            }

            chip.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dpToPx(6), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(new FavoriteChipClickListener(this, fc));
            chip.setOnLongClickListener(new FavoriteChipLongClickListener(this, fc));

            favoritesChipsContainer.addView(chip);
        }
    }

    public static class FavoriteChipClickListener implements View.OnClickListener {
        private final MainActivity act;
        private final WeatherRepository.FavoriteCity city;

        public FavoriteChipClickListener(MainActivity act, WeatherRepository.FavoriteCity city) {
            this.act = act;
            this.city = city;
        }

        @Override
        public void onClick(View v) {
            act.selectLocation(city.name, city.lat, city.lon);
        }
    }

    public static class FavoriteChipLongClickListener implements View.OnLongClickListener {
        private final MainActivity act;
        private final WeatherRepository.FavoriteCity city;

        public FavoriteChipLongClickListener(MainActivity act, WeatherRepository.FavoriteCity city) {
            this.act = act;
            this.city = city;
        }

        @Override
        public boolean onLongClick(View v) {
            act.showRemoveFavoriteDialog(city);
            return true;
        }
    }

    public void showRemoveFavoriteDialog(final WeatherRepository.FavoriteCity fc) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(fc.name)
                .setMessage(getString(R.string.remove_from_favorites) + "?")
                .setPositiveButton("Entfernen", new RemoveFavoriteConfirmListener(this, fc))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static class RemoveFavoriteConfirmListener implements DialogInterface.OnClickListener {
        private final MainActivity act;
        private final WeatherRepository.FavoriteCity city;

        public RemoveFavoriteConfirmListener(MainActivity act, WeatherRepository.FavoriteCity city) {
            this.act = act;
            this.city = city;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            WeatherRepository.removeFavorite(act, city.name);
            act.updateFavoriteButton();
            act.renderFavoritesBar();
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
        updateFavoriteButton();
        renderFavoritesBar();
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
        boolean isGerman = Locale.getDefault().getLanguage().equals("de");

        String locDisplay = (data.locationName != null ? data.locationName : currentLocation.name);
        tvLocationName.setText(locDisplay);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvCardTimestamp.setText("Live " + sdf.format(new Date(data.timestamp)));

        tvHeroIcon.setText(WeatherData.getWeatherIcon(data.weatherCode));
        tvHeroTemp.setText(WeatherData.formatTemp(data.currentTemp, currentUnit));
        tvHeroCondition.setText(WeatherData.getWeatherDescription(data.weatherCode, isGerman));
        tvHeroApparent.setText(getString(R.string.apparent_temp) + ": " + WeatherData.formatTemp(data.apparentTemp, currentUnit));
        tvHeroPrecip.setText(String.format(Locale.US, "💧 %.1f mm", data.precipitation));

        if (!data.daily.isEmpty()) {
            WeatherData.DailyItem today = data.daily.get(0);
            tvHeroMinMax.setText(String.format(Locale.US, "▲ %.0f°  ▼ %.0f°",
                    WeatherData.convertTemp(today.tempMax, currentUnit),
                    WeatherData.convertTemp(today.tempMin, currentUnit)));
        }

        // Telemetry Grid
        tvMetricHumidity.setText(data.relativeHumidity + " %");
        tvMetricWind.setText(String.format(Locale.US, "%.0f km/h • %s", data.windSpeed, data.getWindDirectionText(isGerman)));
        tvMetricPressure.setText(String.format(Locale.US, "%.0f hPa", data.surfacePressure));
        tvMetricCloud.setText(data.cloudCover + " %");

        // Solar-PV Card
        tvPvCurrent.setText(String.format(Locale.US, "⚡ %.2f kW", data.pvPowerKw));
        tvPvEnergy.setText(String.format(Locale.US, "🔋 %.1f kWh", data.pvEnergyKwhToday));

        // Feature 2: Smart Weather Alerts
        WeatherData.WeatherAlert alert = data.getPrimaryAlert(isGerman);
        tvAlertIcon.setText(alert.icon);
        tvAlertTitle.setText(alert.title);
        tvAlertDesc.setText(alert.description);

        if (alert.severity == 2) {
            alertCardLayout.setBackgroundResource(R.drawable.card_bg_alert_severe);
            tvAlertBadge.setText(isGerman ? "Warnung" : "Warning");
            tvAlertBadge.setTextColor(getColor(R.color.brand_red));
        } else if (alert.severity == 1) {
            alertCardLayout.setBackgroundResource(R.drawable.card_bg_alert_warning);
            tvAlertBadge.setText(isGerman ? "Hinweis" : "Advisory");
            tvAlertBadge.setTextColor(getColor(R.color.brand_amber));
        } else {
            alertCardLayout.setBackgroundResource(R.drawable.card_bg_alert_calm);
            tvAlertBadge.setText(isGerman ? "Normal" : "Calm");
            tvAlertBadge.setTextColor(getColor(R.color.accent_emerald));
        }

        // Feature 3: Astronomy & Sun Dashboard
        tvAstroSunrise.setText(data.getSunriseFormatted() + " Uhr");
        tvAstroSunset.setText(data.getSunsetFormatted() + " Uhr");
        tvAstroDaylight.setText(data.getDaylightDuration());
        tvAstroRadiation.setText(data.getPeakRadiationText(isGerman));

        renderHourly(data.hourly);
        renderDaily(data.daily);
        updateFavoriteButton();
    }

    private void renderHourly(List<WeatherData.HourlyItem> hourly) {
        hourlyForecastContainer.removeAllViews();
        if (hourly == null) return;

        for (WeatherData.HourlyItem item : hourly) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
            card.setBackgroundResource(R.drawable.hourly_card_bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dpToPx(76),
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

            TextView tvRain = new TextView(this);
            tvRain.setText(String.format(Locale.US, "💧 %d%%", item.rainProb));
            tvRain.setTextSize(10f);
            tvRain.setTextColor(getColor(R.color.accent_sky));
            card.addView(tvRain);

            if (item.radiation > 15.0) {
                double kw = WeatherData.calcPvPower(item.radiation, item.temp);
                TextView tvPv = new TextView(this);
                tvPv.setText(String.format(Locale.US, "⚡%.1f", kw));
                tvPv.setTextSize(9f);
                tvPv.setTextColor(getColor(R.color.pv_accent));
                card.addView(tvPv);
            }

            hourlyForecastContainer.addView(card);
        }
    }

    private void renderDaily(List<WeatherData.DailyItem> daily) {
        dailyForecastContainer.removeAllViews();
        if (daily == null) return;

        boolean isGerman = Locale.getDefault().getLanguage().equals("de");

        for (int i = 0; i < daily.size(); i++) {
            WeatherData.DailyItem item = daily.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(10), 0, dpToPx(10));

            TextView tvDate = new TextView(this);
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(75), LinearLayout.LayoutParams.WRAP_CONTENT));
            String dateLabel = (i == 0) ? (isGerman ? "Heute" : "Today") : item.date;
            tvDate.setText(dateLabel);
            tvDate.setTextSize(13f);
            tvDate.setTypeface(null, Typeface.BOLD);
            tvDate.setTextColor(getColor(R.color.text_primary));
            row.addView(tvDate);

            TextView tvIcon = new TextView(this);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvIcon.setText(WeatherData.getWeatherIcon(item.weatherCode));
            tvIcon.setTextSize(20f);
            row.addView(tvIcon);

            TextView tvDesc = new TextView(this);
            tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            tvDesc.setText(WeatherData.getWeatherDescription(item.weatherCode, isGerman));
            tvDesc.setTextSize(12f);
            tvDesc.setTextColor(getColor(R.color.text_secondary));
            row.addView(tvDesc);

            TextView tvPrecip = new TextView(this);
            tvPrecip.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(55), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvPrecip.setText(String.format(Locale.US, "💧 %.1f", item.precipSum));
            tvPrecip.setTextSize(11f);
            tvPrecip.setTextColor(getColor(R.color.accent_sky));
            row.addView(tvPrecip);

            TextView tvTemp = new TextView(this);
            tvTemp.setText(String.format(Locale.US, "▲%.0f° ▼%.0f°",
                    WeatherData.convertTemp(item.tempMax, currentUnit),
                    WeatherData.convertTemp(item.tempMin, currentUnit)));
            tvTemp.setTextSize(12f);
            tvTemp.setTypeface(null, Typeface.BOLD);
            tvTemp.setTextColor(getColor(R.color.text_primary));
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
                item.setGravity(Gravity.CENTER_VERTICAL);
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
            act.selectLocation(chosenName, city.latitude, city.longitude);
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
