package com.ejemplo.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Gestor de configuración del bot
 * Maneja la carga y validación de propiedades desde config.properties
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";

    private final Properties properties;

    public ConfigManager() {
        this.properties = new Properties();
        loadConfiguration();
    }

    /**
     * Carga la configuración desde el archivo properties
     */
    private void loadConfiguration() {
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            logger.info("✅ Configuración cargada desde {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("❌ Error al cargar configuración desde {}: {}", CONFIG_FILE, e.getMessage());
            throw new RuntimeException("No se pudo cargar la configuración", e);
        }
    }

    /**
     * Valida que las configuraciones críticas estén presentes
     */
    public boolean validateConfiguration() {
        boolean isValid = true;

        if (getBotToken().trim().isEmpty() || getBotToken().equals("TOKEN_AQUI")) {
            logger.error("❌ Token del bot no configurado correctamente");
            isValid = false;
        }

        if (getWeatherApiKey().equals("API_KEY_CLIMA")) {
            logger.warn("⚠️  API key del clima no configurada - comando !clima no funcionará");
        }

        if (getNewsApiKey().equals("API_KEY_NOTICIAS")) {
            logger.warn("⚠️  API key de noticias no configurada - comando !noticias no funcionará");
        }

        return isValid;
    }

    // Getters para todas las propiedades
    public String getBotToken() {
        return properties.getProperty("bot.token", "");
    }

    public String getWeatherApiKey() {
        return properties.getProperty("weather.api.key", "");
    }

    public String getNewsApiKey() {
        return properties.getProperty("news.api.key", "");
    }

    public String getBotPrefix() {
        return properties.getProperty("bot.prefix", "!");
    }

    /**
     * Obtiene una propiedad personalizada
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}