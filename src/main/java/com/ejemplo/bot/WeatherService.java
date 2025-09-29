package com.ejemplo.bot;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather";

    private final ConfigManager config;
    private final OkHttpClient httpClient;

    public WeatherService(ConfigManager config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public String getWeather(String city) throws Exception {
        String apiKey = config.getWeatherApiKey();

        if (apiKey.isEmpty() || apiKey.equals("API_KEY_CLIMA")) {
            return "❌ API key del clima no configurada. Contacta al administrador.";
        }

        String url = String.format("%s?q=%s&appid=%s&units=metric&lang=es",
                WEATHER_API_URL, city, apiKey);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    return "❌ Ciudad no encontrada: " + city;
                }
                throw new IOException("Error en la API: " + response.code());
            }

            String responseBody = response.body().string();
            return formatWeatherResponse(responseBody, city);

        } catch (IOException e) {
            logger.error("Error consultando API del clima: {}", e.getMessage());
            throw new Exception("Error conectando con el servicio del clima");
        }
    }

    private String formatWeatherResponse(String jsonResponse, String city) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject main = json.getJSONObject("main");
            JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
            JSONObject wind = json.getJSONObject("wind");
            JSONObject sys = json.getJSONObject("sys");

            double temp = main.getDouble("temp");
            double feelsLike = main.getDouble("feels_like");
            int humidity = main.getInt("humidity");
            double pressure = main.getDouble("pressure");
            String description = weather.getString("description");
            double windSpeed = wind.optDouble("speed", 0);
            String country = sys.getString("country");

            StringBuilder result = new StringBuilder();
            result.append("🌤️ **Clima en ").append(city).append(", ").append(country).append("**\n\n");
            result.append("🌡️ **Temperatura:** ").append(Math.round(temp)).append("°C\n");
            result.append("🤲 **Sensación térmica:** ").append(Math.round(feelsLike)).append("°C\n");
            result.append("📝 **Descripción:** ").append(capitalize(description)).append("\n");
            result.append("💧 **Humedad:** ").append(humidity).append("%\n");
            result.append("🏔️ **Presión:** ").append(Math.round(pressure)).append(" hPa\n");
            result.append("💨 **Viento:** ").append(String.format("%.1f", windSpeed)).append(" m/s");

            return result.toString();

        } catch (Exception e) {
            logger.error("Error formateando respuesta del clima: {}", e.getMessage());
            return "❌ Error procesando información del clima.";
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}