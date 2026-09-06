/**
 * wetter-site — Interaktive Wetter-Station & Telemetrie-Engine
 * Entwickelt für Jeremy Benz (@benzjeremy) | GPL-3.0
 * 100% Vanilla JS | Keine externen Frameworks | Pure Canvas Charts
 */

(() => {
  'use strict';

  // --- State ---
  const state = {
    unit: 'celsius', // 'celsius' | 'fahrenheit' | 'kelvin'
    chartMode: 'temp', // 'temp' | 'rain'
    currentLocation: null,
    weatherData: null,
    hoveredIndex: -1,
  };

  // --- WMO Weather Code Map ---
  const WMO_MAP = {
    0: { desc: 'Klarer Himmel', icon: '☀️', mood: 'clear' },
    1: { desc: 'Überwiegend klar', icon: '🌤️', mood: 'clear' },
    2: { desc: 'Teilweise bewölkt', icon: '⛅', mood: 'cloudy' },
    3: { desc: 'Bedeckt', icon: '☁️', mood: 'cloudy' },
    45: { desc: 'Nebel', icon: '🌫️', mood: 'fog' },
    48: { desc: 'Reifnebel', icon: '🌫️', mood: 'fog' },
    51: { desc: 'Leichter Nieselregen', icon: '🌦️', mood: 'rain' },
    53: { desc: 'Mäßiger Nieselregen', icon: '🌦️', mood: 'rain' },
    55: { desc: 'Dichter Nieselregen', icon: '🌧️', mood: 'rain' },
    56: { desc: 'Gefrierender Niesel', icon: '🌨️', mood: 'snow' },
    57: { desc: 'Starker gefrierender Niesel', icon: '🌨️', mood: 'snow' },
    61: { desc: 'Leichter Regen', icon: '🌦️', mood: 'rain' },
    63: { desc: 'Mäßiger Regen', icon: '🌧️', mood: 'rain' },
    65: { desc: 'Starker Regen', icon: '🌧️', mood: 'rain' },
    66: { desc: 'Gefrierender Regen', icon: '🌨️', mood: 'snow' },
    67: { desc: 'Starker gefrierender Regen', icon: '🌨️', mood: 'snow' },
    71: { desc: 'Leichter Schneefall', icon: '🌨️', mood: 'snow' },
    73: { desc: 'Mäßiger Schneefall', icon: '❄️', mood: 'snow' },
    75: { desc: 'Starker Schneefall', icon: '❄️', mood: 'snow' },
    77: { desc: 'Schneegriesel', icon: '🌨️', mood: 'snow' },
    80: { desc: 'Leichte Regenschauer', icon: '🌦️', mood: 'rain' },
    81: { desc: 'Mäßige Regenschauer', icon: '🌧️', mood: 'rain' },
    82: { desc: 'Heftige Regenschauer', icon: '⛈️', mood: 'thunder' },
    85: { desc: 'Leichte Schneeschauer', icon: '🌨️', mood: 'snow' },
    86: { desc: 'Starke Schneeschauer', icon: '❄️', mood: 'snow' },
    95: { desc: 'Gewitter', icon: '⛈️', mood: 'thunder' },
    96: { desc: 'Gewitter mit leichtem Hagel', icon: '⛈️', mood: 'thunder' },
    99: { desc: 'Gewitter mit starkem Hagel', icon: '⛈️', mood: 'thunder' },
  };

  // --- Conversion Helpers ---
  function toUnit(celsius, unit = state.unit) {
    if (celsius === undefined || celsius === null) return 0;
    if (unit === 'fahrenheit') {
      return (celsius * 9) / 5 + 32;
    }
    if (unit === 'kelvin') {
      return celsius + 273.15;
    }
    return celsius;
  }

  function getUnitSymbol(unit = state.unit) {
    if (unit === 'fahrenheit') return '°F';
    if (unit === 'kelvin') return 'K';
    return '°C';
  }

  function formatTemp(celsius, unit = state.unit, decimals = 1) {
    const val = toUnit(celsius, unit);
    return `${val.toFixed(decimals)} ${getUnitSymbol(unit)}`;
  }

  function getWindDirectionText(deg) {
    const directions = [
      'N', 'NNO', 'NO', 'ONO', 
      'O', 'OSO', 'SO', 'SSO', 
      'S', 'SSW', 'SW', 'WSW', 
      'W', 'WNW', 'NW', 'NNW'
    ];
    const index = Math.round((deg % 360) / 22.5) % 16;
    return directions[index] || 'N';
  }

  // --- DOM Elements ---
  const el = {
    launchHeroBtn: document.getElementById('launchHeroBtn'),
    searchForm: document.getElementById('searchForm'),
    locationInput: document.getElementById('locationInput'),
    searchSubmitBtn: document.getElementById('searchSubmitBtn'),
    geoBtn: document.getElementById('geoBtn'),
    clearBtn: document.getElementById('clearBtn'),
    statusBanner: document.getElementById('statusBanner'),
    statusSpinner: document.getElementById('statusSpinner'),
    statusMessage: document.getElementById('statusMessage'),
    weatherDisplay: document.getElementById('weatherDisplay'),

    // Hero weather card
    displayPlace: document.getElementById('displayPlace'),
    displayCoords: document.getElementById('displayCoords'),
    displayTime: document.getElementById('displayTime'),
    displayTemp: document.getElementById('displayTemp'),
    displayUnit: document.getElementById('displayUnit'),
    displayCondition: document.getElementById('displayCondition'),
    displayFeelsLike: document.getElementById('displayFeelsLike'),
    displayMin: document.getElementById('displayMin'),
    displayMax: document.getElementById('displayMax'),
    displayIcon: document.getElementById('displayIcon'),

    // Metrics
    valHumidity: document.getElementById('valHumidity'),
    subHumidity: document.getElementById('subHumidity'),
    valWind: document.getElementById('valWind'),
    windArrow: document.getElementById('windArrow'),
    windDirText: document.getElementById('windDirText'),
    valRain: document.getElementById('valRain'),
    subRain: document.getElementById('subRain'),
    valUV: document.getElementById('valUV'),
    subUV: document.getElementById('subUV'),
    valPressure: document.getElementById('valPressure'),
    subPressure: document.getElementById('subPressure'),
    valSunTimes: document.getElementById('valSunTimes'),

    // Chart
    canvas: document.getElementById('weatherCanvas'),
    chartTooltip: document.getElementById('chartTooltip'),
    modeTempBtn: document.getElementById('modeTempBtn'),
    modeRainBtn: document.getElementById('modeRainBtn'),

    // Forecast
    forecastGrid: document.getElementById('forecastGrid'),
  };

  // --- API Calls ---

  async function geocodeCity(query) {
    const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(query)}&count=1&language=de&format=json`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Geocoding-Dienst antwortete nicht korrekt.');
    const data = await res.json();
    if (!data.results || data.results.length === 0) {
      throw new Error(`Kein Ort für "${query}" gefunden.`);
    }
    const r = data.results[0];
    const countryStr = r.country ? `, ${r.country}` : '';
    const adminStr = r.admin1 && r.admin1 !== r.name ? ` (${r.admin1})` : '';
    return {
      name: `${r.name}${adminStr}${countryStr}`,
      latitude: r.latitude,
      longitude: r.longitude,
    };
  }

  async function reverseGeocode(lat, lon) {
    try {
      const url = `https://geocoding-api.open-meteo.com/v1/search?name=${lat.toFixed(2)},${lon.toFixed(2)}&count=1&language=de&format=json`;
      // Open-Meteo doesn't have native reverse geocode, fallback gracefully:
      return {
        name: `Standort (${lat.toFixed(2)}°, ${lon.toFixed(2)}°)`,
        latitude: lat,
        longitude: lon,
      };
    } catch {
      return {
        name: `Lokaler Standort`,
        latitude: lat,
        longitude: lon,
      };
    }
  }

  async function fetchForecast(lat, lon) {
    const params = new URLSearchParams({
      latitude: lat,
      longitude: lon,
      current: [
        'temperature_2m',
        'relative_humidity_2m',
        'apparent_temperature',
        'is_day',
        'precipitation',
        'weather_code',
        'wind_speed_10m',
        'wind_direction_10m',
        'surface_pressure'
      ].join(','),
      hourly: [
        'temperature_2m',
        'relative_humidity_2m',
        'precipitation_probability',
        'precipitation',
        'weather_code',
        'wind_speed_10m'
      ].join(','),
      daily: [
        'weather_code',
        'temperature_2m_max',
        'temperature_2m_min',
        'apparent_temperature_max',
        'apparent_temperature_min',
        'sunrise',
        'sunset',
        'uv_index_max',
        'precipitation_sum',
        'precipitation_probability_max'
      ].join(','),
      timezone: 'auto'
    });

    const url = `https://api.open-meteo.com/v1/forecast?${params.toString()}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Wetterdaten konnten nicht abgerufen werden.');
    return await res.json();
  }

  // --- Render Functions ---

  function renderCurrentWeather() {
    if (!state.weatherData || !state.currentLocation) return;

    const data = state.weatherData;
    const cur = data.current;
    const daily = data.daily;
    const wmo = WMO_MAP[cur.weather_code] || { desc: 'Unbekannt', icon: '❓', mood: 'cloudy' };

    // Place & Coords
    el.displayPlace.textContent = state.currentLocation.name;
    el.displayCoords.textContent = `${state.currentLocation.latitude.toFixed(2)}° N, ${state.currentLocation.longitude.toFixed(2)}° E`;

    // Time
    const now = new Date();
    el.displayTime.textContent = `Stand: ${now.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })} Uhr · Zeitzone: ${data.timezone}`;

    // Main Temp & Unit
    const tempVal = toUnit(cur.temperature_2m, state.unit);
    el.displayTemp.textContent = tempVal.toFixed(1);
    el.displayUnit.textContent = getUnitSymbol(state.unit);

    // Condition & Icon
    el.displayCondition.textContent = wmo.desc;
    el.displayIcon.textContent = wmo.icon;

    // Feels like, min, max
    el.displayFeelsLike.textContent = formatTemp(cur.apparent_temperature, state.unit);
    el.displayMin.textContent = formatTemp(daily.temperature_2m_min[0], state.unit);
    el.displayMax.textContent = formatTemp(daily.temperature_2m_max[0], state.unit);

    // Metrics
    // 1. Humidity
    el.valHumidity.textContent = `${cur.relative_humidity_2m}%`;
    if (cur.relative_humidity_2m < 40) {
      el.subHumidity.textContent = 'Eher trockene Luft';
    } else if (cur.relative_humidity_2m <= 65) {
      el.subHumidity.textContent = 'Optimaler Wohlfühlbereich';
    } else {
      el.subHumidity.textContent = 'Schwül / Feuchte Luft';
    }

    // 2. Wind
    el.valWind.textContent = `${Math.round(cur.wind_speed_10m)} km/h`;
    const windDir = getWindDirectionText(cur.wind_direction_10m);
    el.windDirText.textContent = `${windDir} (${cur.wind_direction_10m}°)`;
    el.windArrow.style.transform = `rotate(${cur.wind_direction_10m}deg)`;

    // 3. Rain
    const rainSum = cur.precipitation || 0;
    const rainProb = daily.precipitation_probability_max[0] || 0;
    el.valRain.textContent = `${rainSum.toFixed(1)} mm`;
    el.subRain.textContent = `Regenrisiko heute: ${rainProb}%`;

    // 4. UV
    const uv = daily.uv_index_max[0] || 0;
    el.valUV.textContent = `${uv.toFixed(1)} / 11`;
    if (uv <= 2) el.subUV.textContent = 'Niedrig (Kein Schutz nötig)';
    else if (uv <= 5) el.subUV.textContent = 'Mäßig (Schutz empfohlen)';
    else if (uv <= 7) el.subUV.textContent = 'Hoch (Sonnenschutz wichtig!)';
    else el.subUV.textContent = 'Sehr hoch (Schatten suchen)';

    // 5. Pressure
    const pressure = Math.round(cur.surface_pressure || 1013);
    el.valPressure.textContent = `${pressure} hPa`;
    el.subPressure.textContent = pressure >= 1013 ? 'Hochdruckwetter' : 'Tiefdruckgebiet';

    // 6. Sun
    const sunrise = new Date(daily.sunrise[0]).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
    const sunset = new Date(daily.sunset[0]).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
    el.valSunTimes.textContent = `${sunrise} / ${sunset}`;

    // Render 7-day forecast
    renderForecast();

    // Render Canvas Chart
    drawChart();
  }

  function renderForecast() {
    if (!state.weatherData) return;
    const daily = state.weatherData.daily;
    el.forecastGrid.innerHTML = '';

    const count = Math.min(daily.time.length, 7);
    for (let i = 0; i < count; i++) {
      const date = new Date(daily.time[i]);
      const isToday = i === 0;
      const dayName = isToday 
        ? 'Heute' 
        : date.toLocaleDateString('de-DE', { weekday: 'short' });
      const dateStr = date.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' });

      const wmo = WMO_MAP[daily.weather_code[i]] || { desc: 'Heiter', icon: '🌤️' };
      const max = formatTemp(daily.temperature_2m_max[i], state.unit, 0);
      const min = formatTemp(daily.temperature_2m_min[i], state.unit, 0);

      const card = document.createElement('div');
      card.className = 'forecast-card';
      card.innerHTML = `
        <div class="forecast-day">${dayName}</div>
        <div class="forecast-date">${dateStr}</div>
        <div class="forecast-icon">${wmo.icon}</div>
        <div class="forecast-desc">${wmo.desc}</div>
        <div class="forecast-temps">
          <span class="temp-max">${max}</span>
          <span class="temp-min">${min}</span>
        </div>
      `;
      el.forecastGrid.appendChild(card);
    }
  }

  // --- Canvas 24-Hour Graph Drawing Engine ---

  function getHourly24() {
    if (!state.weatherData) return [];
    const hourly = state.weatherData.hourly;
    const now = new Date();
    // Find closest hour index
    let startIdx = 0;
    const nowIso = now.toISOString().slice(0, 13);
    for (let i = 0; i < hourly.time.length; i++) {
      if (hourly.time[i].startsWith(nowIso)) {
        startIdx = i;
        break;
      }
    }

    const points = [];
    for (let i = startIdx; i < startIdx + 24 && i < hourly.time.length; i++) {
      const timeDate = new Date(hourly.time[i]);
      const hourStr = timeDate.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
      const tempC = hourly.temperature_2m[i];
      const rainProb = hourly.precipitation_probability[i] || 0;
      const rainMm = hourly.precipitation[i] || 0;
      const wCode = hourly.weather_code[i];

      points.push({
        time: hourStr,
        tempC,
        tempConverted: toUnit(tempC, state.unit),
        rainProb,
        rainMm,
        wCode,
        wmo: WMO_MAP[wCode] || { desc: 'Heiter', icon: '🌤️' }
      });
    }
    return points;
  }

  function drawChart() {
    const canvas = el.canvas;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const points = getHourly24();
    if (points.length === 0) return;

    // Retina / High-DPI scaling
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    const W = rect.width;
    const H = rect.height;
    ctx.clearRect(0, 0, W, H);

    const padding = { top: 30, right: 35, bottom: 40, left: 45 };
    const chartW = W - padding.left - padding.right;
    const chartH = H - padding.top - padding.bottom;

    const isTemp = state.chartMode === 'temp';

    // Min & Max range
    let values = points.map(p => isTemp ? p.tempConverted : p.rainProb);
    let minVal = Math.min(...values);
    let maxVal = Math.max(...values);

    if (isTemp) {
      const margin = (maxVal - minVal) * 0.15 || 2;
      minVal = Math.floor(minVal - margin);
      maxVal = Math.ceil(maxVal + margin);
    } else {
      minVal = 0;
      maxVal = 100;
    }

    const valRange = maxVal - minVal || 1;

    const getX = (i) => padding.left + (i / (points.length - 1)) * chartW;
    const getY = (val) => padding.top + chartH - ((val - minVal) / valRange) * chartH;

    // Draw Grid Lines (Horizontal)
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.07)';
    ctx.lineWidth = 1;
    const gridSteps = 4;
    for (let i = 0; i <= gridSteps; i++) {
      const stepVal = minVal + (i / gridSteps) * valRange;
      const y = getY(stepVal);
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(W - padding.right, y);
      ctx.stroke();

      // Y-Axis label
      ctx.fillStyle = '#64748b';
      ctx.font = '11px -apple-system, BlinkMacSystemFont, sans-serif';
      ctx.textAlign = 'right';
      ctx.textBaseline = 'middle';
      const label = isTemp 
        ? `${Math.round(stepVal)} ${getUnitSymbol(state.unit)}`
        : `${Math.round(stepVal)}%`;
      ctx.fillText(label, padding.left - 8, y);
    }

    // Draw Curve Path
    ctx.beginPath();
    for (let i = 0; i < points.length; i++) {
      const x = getX(i);
      const y = getY(values[i]);
      if (i === 0) {
        ctx.moveTo(x, y);
      } else {
        const prevX = getX(i - 1);
        const prevY = getY(values[i - 1]);
        const cpX1 = prevX + (x - prevX) / 2;
        const cpY1 = prevY;
        const cpX2 = prevX + (x - prevX) / 2;
        const cpY2 = y;
        ctx.bezierCurveTo(cpX1, cpY1, cpX2, cpY2, x, y);
      }
    }

    // Stroke the curve
    ctx.strokeStyle = isTemp ? '#38bdf8' : '#60a5fa';
    ctx.lineWidth = 3;
    ctx.stroke();

    // Fill Gradient under curve
    ctx.lineTo(getX(points.length - 1), padding.top + chartH);
    ctx.lineTo(getX(0), padding.top + chartH);
    ctx.closePath();

    const grad = ctx.createLinearGradient(0, padding.top, 0, padding.top + chartH);
    if (isTemp) {
      grad.addColorStop(0, 'rgba(56, 189, 248, 0.35)');
      grad.addColorStop(1, 'rgba(56, 189, 248, 0.0)');
    } else {
      grad.addColorStop(0, 'rgba(96, 165, 250, 0.35)');
      grad.addColorStop(1, 'rgba(96, 165, 250, 0.0)');
    }
    ctx.fillStyle = grad;
    ctx.fill();

    // X-Axis labels & Points
    ctx.fillStyle = '#94a3b8';
    ctx.font = '11px -apple-system, BlinkMacSystemFont, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';

    for (let i = 0; i < points.length; i += 3) {
      const x = getX(i);
      ctx.fillText(points[i].time, x, padding.top + chartH + 10);
    }

    // Hover Highlight
    if (state.hoveredIndex >= 0 && state.hoveredIndex < points.length) {
      const i = state.hoveredIndex;
      const x = getX(i);
      const y = getY(values[i]);

      // Vertical line
      ctx.beginPath();
      ctx.strokeStyle = 'rgba(56, 189, 248, 0.6)';
      ctx.setLineDash([3, 3]);
      ctx.moveTo(x, padding.top);
      ctx.lineTo(x, padding.top + chartH);
      ctx.stroke();
      ctx.setLineDash([]);

      // Point dot
      ctx.beginPath();
      ctx.arc(x, y, 6, 0, Math.PI * 2);
      ctx.fillStyle = '#38bdf8';
      ctx.fill();
      ctx.lineWidth = 2;
      ctx.strokeStyle = '#ffffff';
      ctx.stroke();
    }
  }

  // --- Interaction & Tooltip Handling ---

  function handleCanvasMove(e) {
    const canvas = el.canvas;
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const points = getHourly24();
    if (points.length === 0) return;

    const paddingLeft = 45;
    const paddingRight = 35;
    const chartW = rect.width - paddingLeft - paddingRight;

    let closestIdx = Math.round(((mouseX - paddingLeft) / chartW) * (points.length - 1));
    closestIdx = Math.max(0, Math.min(points.length - 1, closestIdx));

    state.hoveredIndex = closestIdx;
    drawChart();

    const p = points[closestIdx];
    const isTemp = state.chartMode === 'temp';

    const valDisplay = isTemp
      ? `${p.tempConverted.toFixed(1)} ${getUnitSymbol(state.unit)}`
      : `${p.rainProb}% (${p.rainMm.toFixed(1)} mm)`;

    el.chartTooltip.innerHTML = `
      <strong>${p.time} Uhr</strong> · ${p.wmo.icon} ${p.wmo.desc}<br>
      <span style="color:${isTemp ? '#38bdf8' : '#60a5fa'}; font-weight:700;">${valDisplay}</span>
    `;
    el.chartTooltip.style.display = 'block';

    const targetX = paddingLeft + (closestIdx / (points.length - 1)) * chartW;
    el.chartTooltip.style.left = `${targetX}px`;
    el.chartTooltip.style.top = `${rect.height * 0.35}px`;
  }

  function handleCanvasLeave() {
    state.hoveredIndex = -1;
    el.chartTooltip.style.display = 'none';
    drawChart();
  }

  // --- Actions & Workflows ---

  function setStatus(msg, isError = false) {
    if (!msg) {
      el.statusBanner.style.display = 'none';
      return;
    }
    el.statusBanner.style.display = 'flex';
    el.statusMessage.textContent = msg;
    if (isError) {
      el.statusBanner.classList.add('error');
      el.statusSpinner.style.display = 'none';
    } else {
      el.statusBanner.classList.remove('error');
      el.statusSpinner.style.display = 'block';
    }
  }

  async function loadWeather(queryOrCoords) {
    try {
      setStatus('Wetterdaten werden in Echtzeit berechnet...');
      el.weatherDisplay.style.display = 'none';

      let loc;
      if (typeof queryOrCoords === 'string') {
        loc = await geocodeCity(queryOrCoords);
      } else {
        loc = queryOrCoords;
      }

      state.currentLocation = loc;
      const weather = await fetchForecast(loc.latitude, loc.longitude);
      state.weatherData = weather;

      setStatus('');
      el.weatherDisplay.style.display = 'block';
      renderCurrentWeather();

    } catch (err) {
      console.error(err);
      setStatus(err.message || 'Ein unerwarteter Fehler ist aufgetreten.', true);
    }
  }

  function useGeolocation() {
    if (!navigator.geolocation) {
      setStatus('Geolokalisierung wird von deinem Browser leider nicht unterstützt.', true);
      return;
    }

    setStatus('Standort wird über deinen Browser ermittelt (GPS)...');
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const { latitude, longitude } = pos.coords;
        const loc = await reverseGeocode(latitude, longitude);
        loadWeather(loc);
      },
      (err) => {
        let msg = 'Standortzugriff abgelehnt oder nicht verfügbar.';
        if (err.code === 1) msg = 'Standortfreigabe im Browser verweigert.';
        setStatus(msg, true);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }

  // --- Event Listeners ---

  function initEvents() {
    // Search Form
    el.searchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const q = el.locationInput.value.trim();
      if (q) loadWeather(q);
    });

    el.searchSubmitBtn.addEventListener('click', () => {
      const q = el.locationInput.value.trim();
      if (q) loadWeather(q);
    });

    // Geolocation
    el.geoBtn.addEventListener('click', useGeolocation);

    // Input changes & Clear
    el.locationInput.addEventListener('input', () => {
      el.clearBtn.style.display = el.locationInput.value ? 'block' : 'none';
    });

    el.clearBtn.addEventListener('click', () => {
      el.locationInput.value = '';
      el.clearBtn.style.display = 'none';
      el.locationInput.focus();
    });

    // City Chips
    document.querySelectorAll('.chip').forEach((chip) => {
      chip.addEventListener('click', () => {
        const city = chip.getAttribute('data-city');
        el.locationInput.value = city;
        el.clearBtn.style.display = 'block';
        loadWeather(city);
      });
    });

    // Unit Selector (°C, °F, K)
    document.querySelectorAll('.unit-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.unit-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        state.unit = btn.getAttribute('data-unit');
        renderCurrentWeather();
      });
    });

    // Chart Mode Buttons (Temp vs Rain)
    el.modeTempBtn.addEventListener('click', () => {
      el.modeTempBtn.classList.add('active');
      el.modeRainBtn.classList.remove('active');
      state.chartMode = 'temp';
      drawChart();
    });

    el.modeRainBtn.addEventListener('click', () => {
      el.modeRainBtn.classList.add('active');
      el.modeTempBtn.classList.remove('active');
      state.chartMode = 'rain';
      drawChart();
    });

    // Canvas Mouse / Touch
    el.canvas.addEventListener('mousemove', handleCanvasMove);
    el.canvas.addEventListener('mouseleave', handleCanvasLeave);
    el.canvas.addEventListener('touchmove', (e) => {
      if (e.touches.length > 0) {
        handleCanvasMove(e.touches[0]);
      }
    });
    el.canvas.addEventListener('touchend', handleCanvasLeave);

    // Launch Hero Button Smooth Scroll & Focus
    el.launchHeroBtn.addEventListener('click', (e) => {
      e.preventDefault();
      document.getElementById('app-station').scrollIntoView({ behavior: 'smooth' });
      setTimeout(() => el.locationInput.focus(), 600);
    });

    // Resize Observer for Chart Responsiveness
    window.addEventListener('resize', () => {
      drawChart();
    });
  }

  function initMobileNav() {
    const toggleBtn = document.getElementById('mobile-toggle');
    const nav = document.getElementById('main-nav');
    if (!toggleBtn || !nav) return;

    toggleBtn.addEventListener('click', () => {
      const isOpen = nav.classList.toggle('open');
      toggleBtn.setAttribute('aria-expanded', isOpen);
      toggleBtn.innerHTML = isOpen ? '✕' : '☰';
    });

    nav.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', () => {
        if (nav.classList.contains('open')) {
          nav.classList.remove('open');
          toggleBtn.setAttribute('aria-expanded', 'false');
          toggleBtn.innerHTML = '☰';
        }
      });
    });

    document.addEventListener('click', (e) => {
      if (nav.classList.contains('open') && !nav.contains(e.target) && !toggleBtn.contains(e.target)) {
        nav.classList.remove('open');
        toggleBtn.setAttribute('aria-expanded', 'false');
        toggleBtn.innerHTML = '☰';
      }
    });
  }

  // --- Initial Launch ---
  document.addEventListener('DOMContentLoaded', () => {
    initEvents();
    initMobileNav();
    // Default load Berlin so user immediately sees full dashboard
    loadWeather('Berlin');
  });

})();
