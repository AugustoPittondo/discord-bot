package com.ejemplo.bot;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para obtener las últimas noticias
 * Utiliza la API de NewsAPI con múltiples fuentes de respaldo
 */
public class NewsService {
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    // URLs de APIs de noticias
    private static final String NEWS_API_URL = "https://newsapi.org/v2/top-headlines?country=ar&pageSize=5&apiKey=";
    private static final String BACKUP_NEWS_URL = "https://newsapi.org/v2/everything?q=Argentina&language=es&sortBy=publishedAt&pageSize=5&apiKey=";

    private final ConfigManager config;
    private final OkHttpClient httpClient;

    public NewsService(ConfigManager config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Obtiene las últimas noticias con múltiples estrategias
     */
    public String getLatestNews() throws Exception {
        String apiKey = config.getNewsApiKey();

        if (apiKey.isEmpty() || apiKey.equals("API_KEY_NOTICIAS")) {
            return "❌ API key de noticias no configurada. Contacta al administrador.";
        }

        // Intentar primero con noticias de Argentina
        try {
            String result = fetchNews(NEWS_API_URL + apiKey, "noticias principales de Argentina");
            if (!result.contains("No se encontraron noticias")) {
                return result;
            }
        } catch (Exception e) {
            logger.warn("Error con API principal de noticias: {}", e.getMessage());
        }

        // Si la primera falló, intentar con búsqueda general
        try {
            String result = fetchNews(BACKUP_NEWS_URL + apiKey, "búsqueda general");
            if (!result.contains("No se encontraron noticias")) {
                return result;
            }
        } catch (Exception e) {
            logger.warn("Error con API de respaldo: {}", e.getMessage());
        }

        // Si ambas fallaron, devolver mensaje informativo
        return "📰 **Servicio de Noticias Temporalmente No Disponible**\n\n" +
                "⚠️ Esto puede ser debido a:\n" +
                "• Límites de la API gratuita de NewsAPI\n" +
                "• Restricciones geográficas\n" +
                "• Mantenimiento del servicio\n\n" +
                "🔄 Intenta nuevamente en unos minutos.";
    }

    /**
     * Realiza la petición HTTP y procesa la respuesta
     */
    private String fetchNews(String url, String source) throws Exception {
        logger.info("Consultando noticias desde: {}", source);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Discord-Bot/1.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 429) {
                    throw new Exception("Límite de requests excedido");
                } else if (response.code() == 401) {
                    throw new Exception("API key inválida");
                } else if (response.code() == 426) {
                    throw new Exception("Upgrade requerido - API gratuita limitada");
                }
                throw new Exception("Error HTTP: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Respuesta recibida, procesando...");
            return formatNewsResponse(responseBody);

        } catch (IOException e) {
            logger.error("Error consultando {}: {}", source, e.getMessage());
            throw new Exception("Error conectando con " + source);
        }
    }

    /**
     * Formatea la respuesta de la API en un mensaje legible
     */
    private String formatNewsResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);

            // Verificar status de la API
            if (json.has("status") && !"ok".equals(json.getString("status"))) {
                String errorMessage = json.optString("message", "Error desconocido");
                logger.error("Error de NewsAPI: {}", errorMessage);
                return "❌ Error del servicio de noticias: " + errorMessage;
            }

            // Verificar que hay artículos
            if (!json.has("articles")) {
                logger.warn("Respuesta no contiene artículos: {}", jsonResponse);
                return "📰 No se encontraron noticias en este momento.";
            }

            JSONArray articles = json.getJSONArray("articles");
            int totalResults = json.optInt("totalResults", articles.length());

            logger.info("Total de noticias encontradas: {}", totalResults);

            if (articles.length() == 0) {
                return "📰 No se encontraron noticias en este momento.";
            }

            StringBuilder result = new StringBuilder();
            result.append("📰 **Últimas Noticias de Argentina**\n\n");

            int validArticles = 0;
            for (int i = 0; i < Math.min(3, articles.length()); i++) {
                JSONObject article = articles.getJSONObject(i);

                String title = article.optString("title", "");
                String source = "";
                String url = article.optString("url", "");

                // Obtener fuente
                if (article.has("source") && !article.isNull("source")) {
                    JSONObject sourceObj = article.getJSONObject("source");
                    source = sourceObj.optString("name", "Desconocida");
                }

                // Filtrar artículos sin título o con títulos genéricos
                if (title.isEmpty() || title.equals("null") || title.toLowerCase().contains("[removed]")) {
                    continue;
                }

                // Acortar título si es muy largo
                if (title.length() > 100) {
                    title = title.substring(0, 97) + "...";
                }

                validArticles++;
                result.append("**").append(validArticles).append(". ").append(title).append("**\n");
                result.append("📺 ").append(source);

                if (!url.isEmpty() && !url.equals("null")) {
                    result.append(" • [Ver más](").append(url).append(")");
                }

                result.append("\n\n");

                // Verificar que no excedamos 1800 caracteres (dejamos margen)
                if (result.length() > 1800) {
                    break;
                }

                // Limitar a 3 artículos válidos
                if (validArticles >= 3) break;
            }

            if (validArticles == 0) {
                return "📰 No se encontraron noticias válidas en este momento.";
            }

            result.append("🔍 *Encontradas ").append(totalResults).append(" noticias*");

            return result.toString();

        } catch (Exception e) {
            logger.error("Error formateando respuesta de noticias: {}", e.getMessage(), e);
            return "❌ Error procesando las noticias. Intenta nuevamente.";
        }
    }
}