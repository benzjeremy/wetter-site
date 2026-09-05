# ☀️ wetter-site — Moderne Wetter-Station & Interaktive Telemetrie

[![Website](https://img.shields.io/badge/Website-Live-38bdf8?style=for-the-badge&logo=googlechrome&logoColor=black)](https://benzjeremy.github.io/wetter-site/)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](LICENSE)
[![Open-Meteo](https://img.shields.io/badge/API-Open--Meteo-orange?style=for-the-badge)](https://open-meteo.com)
[![Zero-Bloat](https://img.shields.io/badge/Framework-100%25%20Vanilla%20JS-success?style=for-the-badge)](https://benzjeremy.github.io/wetter-site/)

Die offizielle, ultra-schnelle Wetter-Station von **Jeremy Benz**. Entwickelt mit purem, semantischem HTML5, modernem CSS3 (Dark Theme mit Glassmorphism-Ästhetik) und nativer HTML5 Canvas-Grafik — **100% frei von schweren Frameworks, blitzschnell und ohne Tracking**.

👉 **Live-Website:** [https://benzjeremy.github.io/wetter-site/](https://benzjeremy.github.io/wetter-site/)

---

## ✨ Features

- ⚡ **Zero-Bloat & Pure Vanilla:** Reines HTML5, CSS3 und Vanilla JS – keine Node-Module, kein Webpack, kein Hydration-Overhead.
- 📈 **Interaktive 24h Canvas-Graphen:** Butterweiche Kurven für stündliche Temperaturen und Niederschlagswahrscheinlichkeiten mit dynamischer Skalierung und Hover-Tooltip.
- 🌡️ **Alle Grad-Einheiten (°C, °F, K):** Nahtloser Wechsel in Echtzeit zwischen **Celsius (°C)**, **Fahrenheit (°F)** und **Kelvin (K)** ohne erneuten Server-Request.
- 🌍 **Weltweites Geocoding:** Sofortige Ortssuche über die datenschutzfreundliche Open-Meteo API.
- 📍 **Clientseitige Geolocation:** 1-Klick-Standortermittlung per GPS – rein lokal im Browser ohne externe Speicherung.
- 📅 **7-Tage-Trend:** Detaillierte Wochenvorhersage mit WMO-Wettersymbolen und Min-/Max-Temperaturbereichen.
- 💨 **Atmosphärische Telemetrie:** Luftfeuchte, Windgeschwindigkeit & -richtung (inkl. Kompassnadel), Niederschlag, UV-Index, Luftdruck und Sonnenauf-/untergangszeiten.
- 🔒 **Privacy by Design:** Keine Tracking-Cookies, keine Werbenetzwerke, keine Server-Datenspeicherung. DSGVO-konform.

---

## 🛠️ Verzeichnisstruktur

```text
wetter-site/
├── index.html         <- Startpage & interaktive Wetter-Station
├── style.css          <- Modern Dark Theme mit Glassmorphism & Responsiveness
├── app.js             <- Reines JS mit Canvas-Engine & Open-Meteo Integration
├── favicon.svg        <- Wetter Favicon
├── icon.svg           <- Projekt-Icon
├── robots.txt         <- Suchmaschinen-Richtlinie
├── sitemap.xml        <- XML-Sitemap
├── .nojekyll          <- GitHub Pages Auslieferung ohne Jekyll-Filter
├── README.md          <- Projektdokumentation
└── LICENSE            <- GNU General Public License v3.0
```

---

## 👥 Mitwirkende & Credits

- **Lead Developer & Gründer:** Jeremy Benz ([@benzjeremy](https://github.com/benzjeremy) & [@jbenz1706](https://github.com/jbenz1706))
- **AI-Assistenten (Pair Programming):** Google Antigravity & Claude Code
- **Wetterdaten & Geocoding:** [Open-Meteo](https://open-meteo.com) (Non-Commercial Open Data)

---

## 📄 Lizenz

Dieses Projekt steht unter der **GNU General Public License, Version 3 (GPL-3.0)**.  
Weitere Informationen findest du in der Datei [LICENSE](LICENSE).
