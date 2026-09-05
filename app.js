// Wait for DOM to load
document.addEventListener('DOMContentLoaded', () => {
    const startButton = document.getElementById('startButton');
    const weatherApp = document.getElementById('weatherApp');
    const locationInput = document.getElementById('locationInput');
    const useLocationButton = document.getElementById('useLocation');
    const unitSelect = document.getElementById('unitSelect');
    const fetchWeatherButton = document.getElementById('fetchWeather');
    const weatherResult = document.getElementById('weatherResult');
    const yearSpan = document.getElementById('year');

    // Set current year in footer
    yearSpan.textContent = new Date().getFullYear();

    // Show weather app when start button is clicked
    startButton.addEventListener('click', () => {
        startButton.parentElement.parentElement.style.display = 'none';
        weatherApp.style.display = 'block';
        locationInput.focus();
    });

    // Get user's current location
    useLocationButton.addEventListener('click', () => {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    const { latitude, longitude } = position.coords;
                    // Use reverse geocoding to get a place name? 
                    // For simplicity, we'll just store the coordinates and use them directly with the API.
                    locationInput.value = `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
                    alert('Standort erfasst: ' + locationInput.value);
                },
                (error) => {
                    alert('Fehler beim Erhalten des Standorts: ' + error.message);
                }
            );
        } else {
            alert('Geolokalisierung wird von diesem Browser nicht unterstützt.');
        }
    });

    // Fetch weather data
    fetchWeatherButton.addEventListener('click', () => {
        const location = locationInput.value.trim();
        const unit = unitSelect.value;

        if (!location) {
            alert('Bitte gib einen Ort ein.');
            return;
        }

        // Show loading state
        weatherResult.innerHTML = '<p class="loading">Wetterdaten werden geladen...</p>';
        weatherResult.style.display = 'block';

        // Use Open-Meteo API
        // First, we need to geocode the location to get latitude and longitude.
        // We'll use a free geocoding API: https://geocoding-api.open-meteo.com/v1/search
        const geocodeUrl = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(location)}&count=1&language=de&format=json`;

        fetch(geocodeUrl)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Geocoding API responded with status: ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                if (!data.results || data.results.length === 0) {
                    throw new Error('Ort nicht gefunden. Bitte versuche einen anderen Ort.');
                }
                const { latitude, longitude, name, country, admin1 } = data.results[0];
                const placeName = `${name}${admin1 ? ', ' + admin1 : ''}${country ? ', ' + country : ''}`;

                // Now fetch weather data for the coordinates
                const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto`;

                return fetch(weatherUrl).then(response => {
                    if (!response.ok) {
                        throw new Error('Weather API responded with status: ' + response.status);
                    }
                    return response.json();
                }).then(weatherData => {
                    return { placeName, latitude, longitude, weatherData };
                });
            })
            .then(({ placeName, latitude, longitude, weatherData }) => {
                // Display the weather data
                displayWeather(placeName, latitude, longitude, weatherData, unit);
            })
            .catch(error => {
                weatherResult.innerHTML = `<p class="error">Fehler: ${error.message}</p>`;
                console.error(error);
            });
    });

    function displayWeather(placeName, latitude, longitude, weatherData, unit) {
        const current = weatherData.current;
        const daily = weatherData.daily;

        // Convert temperature based on unit
        const convertTemp = (temp) => {
            if (unit === 'fahrenheit') {
                return (temp * 9/5) + 32;
            } else if (unit === 'kelvin') {
                return temp + 273.15;
            }
            return temp; // celsius
        };

        const currentTemp = convertTemp(current.temperature_2m);
        const feelsLike = convertTemp(current.apparent_temperature);
        const tempMin = convertTemp(daily.temperature_2m_min[0]);
        const tempMax = convertTemp(daily.temperature_2m_max[0]);

        // Weather code to description (simplified)
        const weatherCodeDescriptions = {
            0: "Klarer Himmel",
            1: "Hauptsächlich klar",
            2: "Teilweise bewölkt",
            3: "Bewölkt",
            45: "Nebel",
            48: "Deposierender Nebel",
            51: "Leichter Nieselregen",
            53: "Mäßiger Nieselregen",
            55: "Dichter Nieselregen",
            56: "Leichter gefrierender Nieselregen",
            57: "Dichter gefrierender Nieselregen",
            61: "Leichter Regen",
            63: "Mäßiger Regen",
            65: "Starker Regen",
            66: "Leichter gefrierender Regen",
            67: "Starker gefrierender Regen",
            71: "Leichter Schneefall",
            73: "Mäßiger Schneefall",
            75: "Starker Schneefall",
            77: "Schneekörner",
            80: "Leichter Regenschauer",
            81: "Mäßiger Regenschauer",
            82: "Starker Regenschauer",
            85: "Leichter Schneeschauer",
            86: "Schwerer Schneeschauer",
            95: "Gewitter",
            96: "Gewitter mit Hagel",
            99: "Starker Gewitter mit Hagel"
        };

        const weatherDescription = weatherCodeDescriptions[current.weather_code] || "Unbekannt";

        // Format sunrise and sunset times
        const sunrise = new Date(daily.sunrise[0]).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        const sunset = new Date(daily.sunset[0]).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

        // Build HTML for weather result
        weatherResult.innerHTML = `
            <h3>Wetter für ${placeName}</h3>
            <p><strong>Position:</strong> ${latitude.toFixed(4)}° N, ${longitude.toFixed(4)}° E</p>
            <div class="weather-summary">
                <p><strong>Wetter:</strong> ${weatherDescription}</p>
                <p><strong>Temperatur:</strong> ${currentTemp.toFixed(1)}°${unitToSymbol(unit)}</p>
                <p><strong>Gefühlte Temperatur:</strong> ${feelsLike.toFixed(1)}°${unitToSymbol(unit)}</p>
                <p><strong>Luftfeuchtigkeit:</strong> ${current.relative_humidity_2m}%</p>
                <p><strong>Niederschlag:</strong> ${current.precipitation} mm</p>
                <p><strong>Wind:</strong> ${current.wind_speed_10m} m/s aus Richtung ${current.wind_direction_10m}°</p>
            </div>
            <div class="weather-forecast">
                <h4>Heute</h4>
                <p><strong>Tiefsttemperatur:</strong> ${tempMin.toFixed(1)}°${unitToSymbol(unit)}</p>
                <p><strong>Höchsttemperatur:</strong> ${tempMax.toFixed(1)}°${unitToSymbol(unit)}</p>
                <p><strong>Sonnenaufgang:</strong> ${sunrise}</p>
                <p><strong>Sonnenuntergang:</strong> ${sunset}</p>
            </div>
            <p><em>Daten vom Open-Meteo</em></p>
        `;
        weatherResult.style.display = 'block';
    }

    function unitToSymbol(unit) {
        if (unit === 'fahrenheit') return 'F';
        if (unit === 'kelvin') return 'K';
        return 'C';
    }
});
