package com.ejemplo.bot;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para obtener precios de criptomonedas
 * Utiliza la API gratuita de CoinGecko
 */
public class CryptoService {
    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);
    private static final String CRYPTO_API_URL = "https://api.coingecko.com/api/v3/simple/price" +
            "?ids=bitcoin,ethereum,cardano,polkadot&vs_currencies=usd&include_24hr_change=true";

    private final OkHttpClient httpClient;
    private final NumberFormat currencyFormatter;
    private final NumberFormat percentFormatter;

    public CryptoService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        this.currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        this.percentFormatter = NumberFormat.getPercentInstance(Locale.US);
        this.percentFormatter.setMaximumFractionDigits(2);
    }

    /**
     * Obtiene los precios actuales de las principales criptomonedas
     */
    public String getCryptoPrices() throws Exception {
        Request request = new Request.Builder()
                .url(CRYPTO_API_URL)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error en la API de CoinGecko: " + response.code());
            }

            String responseBody = response.body().string();
            return formatCryptoResponse(responseBody);

        } catch (IOException e) {
            logger.error("Error consultando API de CoinGecko: {}", e.getMessage());
            throw new Exception("Error conectando con el servicio de criptomonedas");
        }
    }

    /**
     * Formatea la respuesta de la API en un mensaje legible
     */
    private String formatCryptoResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);

            StringBuilder result = new StringBuilder();
            result.append("₿ **Precios de Criptomonedas**\n\n");

            // Bitcoin
            if (json.has("bitcoin")) {
                JSONObject btc = json.getJSONObject("bitcoin");
                double price = btc.getDouble("usd");
                double change24h = btc.getDouble("usd_24h_change");
                String changeEmoji = change24h >= 0 ? "📈" : "📉";
                String changeSign = change24h >= 0 ? "+" : "";

                result.append("**🟠 Bitcoin (BTC)**\n");
                result.append("💰 Precio: ").append(currencyFormatter.format(price)).append("\n");
                result.append(changeEmoji).append(" 24h: ").append(changeSign)
                        .append(String.format("%.2f%%", change24h)).append("\n\n");
            }

            // Ethereum
            if (json.has("ethereum")) {
                JSONObject eth = json.getJSONObject("ethereum");
                double price = eth.getDouble("usd");
                double change24h = eth.getDouble("usd_24h_change");
                String changeEmoji = change24h >= 0 ? "📈" : "📉";
                String changeSign = change24h >= 0 ? "+" : "";

                result.append("**🔵 Ethereum (ETH)**\n");
                result.append("💰 Precio: ").append(currencyFormatter.format(price)).append("\n");
                result.append(changeEmoji).append(" 24h: ").append(changeSign)
                        .append(String.format("%.2f%%", change24h)).append("\n\n");
            }

            // Cardano
            if (json.has("cardano")) {
                JSONObject ada = json.getJSONObject("cardano");
                double price = ada.getDouble("usd");
                double change24h = ada.getDouble("usd_24h_change");
                String changeEmoji = change24h >= 0 ? "📈" : "📉";
                String changeSign = change24h >= 0 ? "+" : "";

                result.append("**🔴 Cardano (ADA)**\n");
                result.append("💰 Precio: $").append(String.format("%.4f", price)).append("\n");
                result.append(changeEmoji).append(" 24h: ").append(changeSign)
                        .append(String.format("%.2f%%", change24h)).append("\n\n");
            }

            // Polkadot
            if (json.has("polkadot")) {
                JSONObject dot = json.getJSONObject("polkadot");
                double price = dot.getDouble("usd");
                double change24h = dot.getDouble("usd_24h_change");
                String changeEmoji = change24h >= 0 ? "📈" : "📉";
                String changeSign = change24h >= 0 ? "+" : "";

                result.append("**⚫ Polkadot (DOT)**\n");
                result.append("💰 Precio: ").append(currencyFormatter.format(price)).append("\n");
                result.append(changeEmoji).append(" 24h: ").append(changeSign)
                        .append(String.format("%.2f%%", change24h)).append("\n\n");
            }

            result.append("📊 *Datos proporcionados por CoinGecko*");

            return result.toString();

        } catch (Exception e) {
            logger.error("Error formateando respuesta de criptomonedas: {}", e.getMessage());
            return "❌ Error procesando precios de criptomonedas.";
        }
    }
}