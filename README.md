# ☀️ Wetter – Native Android Weather & Solar Station

**Projekt eingestellt.** v1.6.1 (Build 161) ist die letzte offizielle Version. Forks und Weiterentwicklung sind unter der beiliegenden GPLv3 willkommen. [Downloadarchiv](https://downloads.benzjeremy.pp.ua/wetter/).


Wetter zeigt Temperatur und Vorhersage für einen gewählten Ort. Unter der aktuellen Temperatur stehen der Stundenverlauf und die nächsten sieben Tage. Wind, Niederschlag, Sonnenzeiten und geschätzter Solarertrag sind nach Themen gegliedert. Android wählt die helle oder dunkle Darstellung anhand der Systemeinstellung.

Die Ortssuche und die Vorhersage nutzen Open-Meteo. Suchbegriff oder Ortskoordinaten werden direkt an dessen APIs übertragen. Das Widget unterstützt regelmäßige Aktualisierungen. Impressum, Kontakt, Datenverarbeitung und Lizenz stehen in den App-Einstellungen.

## 🛠️ Building from Source

The build pipeline uses the standalone Android SDK CLI toolchain without Gradle:

```bash
export WETTER_KEYSTORE=/path/to/private.keystore
# Set WETTER_KEY_PASSWORD in your private build environment.
cd android
./build_apk.sh
```

Requirements:
- Android SDK Build-Tools 34.0.0 (`aapt2`, `d8`, `zipalign`, `apksigner`)
- Android Platform 34 (`android.jar`)
- JDK 17+ (compiles to `--release 8`)

Output binary:
`android/wetter-v1.6.1.apk`

---

## 📜 License

GNU General Public License v3.0 (GNU GPL-3.0). See [LICENSE](LICENSE) for details.
