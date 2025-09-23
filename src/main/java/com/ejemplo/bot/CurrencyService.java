package com.ejemplo.bot;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para obtener cotizaciones de monedas
 * Utiliza la API gratuita de BluelyTics para cotizaciones del dólar
 */
public class CurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);
    private static final String DOLLAR_API_URL = "https://api.bluelytics.com.ar/v2/latest";

    private final OkHttpClient httpClient;

    public CurrencyService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Obtiene la cotización actual del dólar oficial y blue
     */
    public String getDollarRate() throws Exception {
        Request request = new Request.Builder()
                .url(DOLLAR_API_URL)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error en la API de BluelyTics: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Respuesta de BluelyTics: {}", responseBody);
            return formatDollarResponse(responseBody);

        } catch (IOException e) {
            logger.error("Error consultando API de BluelyTics: {}", e.getMessage());
            throw new Exception("Error conectando con el servicio de cotizaciones");
        }
    }

    /**
     * Formatea la respuesta de la API en un mensaje legible
     */
    private String formatDollarResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);

            // Verificar que los objetos existan
            if (!json.has("oficial") || !json.has("blue")) {
                logger.error("Respuesta JSON no contiene datos esperados: {}", jsonResponse);
                return "❌ Error: Datos de cotización no disponibles.";
            }

            // Dólar oficial
            JSONObject oficial = json.getJSONObject("oficial");
            double oficialCompra = oficial.optDouble("value_buy", 0);
            double oficialVenta = oficial.optDouble("value_sell", 0);

            // Dólar blue
            JSONObject blue = json.getJSONObject("blue");
            double blueCompra = blue.optDouble("value_buy", 0);
            double blueVenta = blue.optDouble("value_sell", 0);

            // Validar que tenemos datos válidos
            if (oficialVenta == 0 || blueVenta == 0) {
                logger.error("Valores de cotización inválidos - Oficial: {}, Blue: {}", oficialVenta, blueVenta);
                return "❌ Error: Valores de cotización no disponibles en este momento.";
            }

            // Calcular brecha
            double brecha = ((blueVenta - oficialVenta) / oficialVenta) * 100;

            // Fecha de última actualización
            String lastUpdate = json.optString("last_update", "");
            String fechaFormateada = "No disponible";

            if (!lastUpdate.isEmpty() && !lastUpdate.equals("null")) {
                try {
                    Date updateDate = new Date(lastUpdate);
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    fechaFormateada = formatter.format(updateDate);
                } catch (Exception e) {
                    logger.warn("Error parseando fecha: {}", lastUpdate);
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("💵 **Cotización del Dólar en Argentina**\n\n");

            result.append("🏛️ **Dólar Oficial**\n");
            result.append("💰 Compra: $").append(String.format("%.2f", oficialCompra)).append("\n");
            result.append("💸 Venta: $").append(String.format("%.2f", oficialVenta)).append("\n\n");

            result.append("🔵 **Dólar Blue**\n");
            result.append("💰 Compra: $").append(String.format("%.2f", blueCompra)).append("\n");
            result.append("💸 Venta: $").append(String.format("%.2f", blueVenta)).append("\n\n");

            result.append("📊 **Brecha:** ").append(String.format("%.1f%%", brecha));

            String brechaEmoji = brecha > 50 ? " 🔴" : brecha > 25 ? " 🟡" : " 🟢";
            result.append(brechaEmoji).append("\n\n");

            result.append("🕐 *Última actualización: ").append(fechaFormateada).append("*\n");
            result.append("📊 *Fuente: BluelyTics*");

            return result.toString();

        } catch (Exception e) {
            logger.error("Error formateando respuesta del dólar: {}", e.getMessage(), e);
            return "❌ Error procesando cotización del dólar. Intenta nuevamente en unos minutos.";
        }
    }
}