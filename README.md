# ☀️ Wetter – Native Android Weather & Solar Station

A lightweight, zero-bloat, privacy-respecting native Android application and interactive home screen widget that provides real-time weather forecasts and photovoltaic (PV) solar yield estimations.

Directly powered by the open [Open-Meteo](https://open-meteo.com/) API — completely independent, zero trackers, and no intermediate relay servers.

---

## ✨ Features

- **Native & Lightweight:** 100% pure native Java architecture (`< 90 KB` APK size). Instant launch, zero WebView overhead, and battery-friendly background execution.
- **Home Screen Widget (4×2 & 4×1):** Displays real-time temperature, condition emoji, min/max range, solar PV yield, and an instant refresh button.
- **Solar PV Telemetry:** Physics-based daily yield and power estimation ($P_{pv}$) for standard 5 kWp residential installations.
- **Strict Privacy:** Zero tracking, zero analytics, zero advertising IDs. Fully functional without GPS location permissions (geocoded city search).
- **24-Hour Trend & 7-Day Forecast:** Detailed hourly temperature, precipitation probability, and multi-day meteorological trends.
- **Multi-Unit Support:** Dynamic switching between Celsius (°C), Fahrenheit (°F), and Kelvin (K).

---

## 🎨 Unified Design System

The app and widget conform to the unified Jeremy Benz Design System:
- **Dark Background:** `#0b1220` with `#090e18` station frame
- **Card Surfaces:** `#0d1524` with subtle `#263750` borders and 10 dp corner radii
- **Interaction Accents:** `#8eb5ff` (Dark) / `#2563eb` (Light)
- **Solar Yield Highlighting:** `#fbbf24` text with `#f59e0b` curve

---

## 🛠️ Building from Source

The build pipeline uses the standalone Android SDK CLI toolchain without Gradle:

```bash
cd android
./build_apk.sh
```

Requirements:
- Android SDK Build-Tools 34.0.0 (`aapt2`, `d8`, `zipalign`, `apksigner`)
- Android Platform 34 (`android.jar`)
- JDK 17+ (compiles to `--release 8`)

Output binary:
`android/wetter-v1.3.apk`

---

## 📜 License

GNU General Public License v3.0 (GNU GPL-3.0). See [LICENSE](LICENSE) for details.
