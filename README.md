# ☀️ Weather (`wetter-site`) — Privacy-First Weather & Solar PV Ecosystem

[![F-Droid](https://img.shields.io/badge/F--Droid-myfdroid-blue?style=for-the-badge&logo=fdroid)](https://benzjeremy.github.io/myfdroid/)
[![Android](https://img.shields.io/badge/Android-SDK%2021--34-brightgreen?style=for-the-badge&logo=android)](https://benzjeremy.github.io/myfdroid/repo/wetter-v1.0.apk)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](LICENSE)
[![API: Open-Meteo](https://img.shields.io/badge/API-Open--Meteo-orange?style=for-the-badge)](https://open-meteo.com)
[![Web Live](https://img.shields.io/badge/Website-Live-38bdf8?style=for-the-badge&logo=googlechrome&logoColor=black)](https://benzjeremy.github.io/wetter-site/)

A privacy-focused, zero-telemetry weather forecasting and solar photovoltaic (PV) yield prediction ecosystem developed by **Jeremy Benz**. Designed with zero proprietary tracking, zero analytics, no advertising, and no relay server requirements.

---

## 🌟 Highlights

- 📱 **Native Android Application (`com.benzjeremy.wetter`)**: Pure native Java implementation (zero WebView overhead), high-efficiency battery usage, and offline-first persistence.
- 📲 **Interactive Home Screen Widget**: Resizable 4x2 / 4x1 Android App Widget displaying current temperature, weather conditions, daily min/max, solar PV output, and a one-tap refresh button.
- ⚡ **Direct Open-Meteo API**: Communicates directly with the public Open-Meteo weather and geocoding endpoints without any intermediary relay servers, tokens, or personal registration.
- ☀️ **Solar PV Yield Modeling**: Real-time rooftop and balcony solar power generation estimation based on global horizontal irradiance, cloud cover, and ambient temperature (5 kWp reference model).
- 🌍 **Global Geocoding & Unit Switching**: Instant city search across the globe with local persistence, supporting Celsius (°C), Fahrenheit (°F), and Kelvin (K).
- 🔒 **Privacy by Design**: Zero telemetry, no third-party trackers, no location tracking logs. Operates seamlessly on de-googled operating systems (GrapheneOS, CalyxOS, LineageOS).
- 📦 **Distributed via `myfdroid`**: Cryptographically signed APKs indexed in the official Jeremy Benz F-Droid repository.

---

## 🏛️ Ecosystem Architecture

The project follows the standard **3-Web & 3-Branch Architecture**:

| Branch | Purpose | Scope |
|---|---|---|
| `main` | Source code repository | 100% English native Android source, build scripts, fastlane metadata, CI workflows |
| `web` | Static GitHub Pages deployment | 100% Bilingual (DE/EN) Showcase (`/`), Web-Wiki (`/wiki/`), and Web-App (`/app/`) |

### Android App Directory Layout

```text
android/
├── AndroidManifest.xml                  <- App configuration, permissions & widget declarations
├── app/src/main/
│   ├── java/com/benzjeremy/wetter/
│   │   ├── MainActivity.java            <- Native dashboard, forecast lists & settings
│   │   ├── WeatherData.java             <- Data model, WMO parser & solar PV physics formulas
│   │   ├── WeatherRepository.java       <- Direct HTTP connection to Open-Meteo & local cache
│   │   └── WeatherWidgetProvider.java   <- AppWidgetProvider for home screen widget
│   └── res/
│       ├── layout/                      <- activity_main.xml, widget_weather.xml
│       ├── values/                      <- colors.xml, styles.xml, strings.xml (DE/EN)
│       ├── xml/                         <- widget_weather_info.xml
│       └── mipmap-*/                    <- App & widget launcher icons
├── build_apk.sh                         <- Reproducible CLI build pipeline (aapt2, d8, apksigner)
└── wetter-v1.0.apk                      <- Signed release APK (74 KB)
```

---

## ☀️ Solar PV Physical Calculation Model

The estimated instantaneous solar power output $P_{pv}$ (in Watts) is modeled using solar elevation angle, global horizontal irradiance (GHI), ambient temperature, and cloud attenuation:

$$P_{pv} = P_{peak} \cdot \left( \frac{G_{eff}}{1000\,\text{W/m}^2} \right) \cdot \left[ 1 + \gamma \cdot (T_{cell} - 25^\circ\text{C}) \right] \cdot \eta_{inverter}$$

Where:
- $P_{peak} = 5000\,\text{W}$ (standard 5.0 kWp residential photovoltaic installation)
- $G_{eff} = G_{clear\_sky} \cdot (1 - 0.75 \cdot (\text{cloud\_cover} / 100)^{3.4})$
- $\gamma = -0.0038\,/\,^\circ\text{C}$ (temperature power coefficient for monocrystalline silicon)
- $T_{cell} = T_{ambient} + \frac{NOCT - 20}{800} \cdot G_{eff}$
- $\eta_{inverter} = 0.96$ (inverter conversion efficiency)

Nighttime irradiance is strictly clamped to $0.0\,\text{W}$ when the sun is below the horizon.

---

## 🛠️ Building from Source

### Prerequisites
- Java Development Kit (JDK 17 or JDK 21)
- Android SDK Build-Tools (e.g., version 34.0.0 or 35.0.0)
- Android Platform SDK (`android-34`)

### Build Command
Run the standalone build script from the repository root:

```bash
chmod +x android/build_apk.sh
./android/build_apk.sh
```

The script performs:
1. Resource compilation using `aapt2 compile` and `aapt2 link`.
2. Java compilation using `javac --release 8` against `android.jar`.
3. Bytecode desugaring and DEX conversion via `d8`.
4. Package alignment with `zipalign -p 4`.
5. Cryptographic signing with `apksigner` (v1, v2, v3 schemes).

---

## 📲 F-Droid Repository Installation

Add the official repository to F-Droid, Droid-ify, or Neo Store:

```text
https://benzjeremy.github.io/myfdroid/repo?fingerprint=A2AD027C856AE9E111B17A55DF5303BB423DBF8D447DC981BDB0E21346963CB7
```

Direct APK download is also available at:
`https://benzjeremy.github.io/myfdroid/repo/wetter-v1.0.apk`

---

## 👥 Contributors & Credits

- **Author & Maintainer:** Jeremy Benz ([@benzjeremy](https://github.com/benzjeremy))
- **AI Pair Programming:** Google Antigravity
- **Weather Data & Geocoding:** [Open-Meteo](https://open-meteo.com) (Open Data under CC-BY 4.0)

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for details.
