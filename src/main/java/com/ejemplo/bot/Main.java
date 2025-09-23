package com.ejemplo.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal del bot de Discord
 * Inicializa y configura el bot con todos sus servicios
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // Cargar configuración
            ConfigManager config = new ConfigManager();

            // Validar configuración crítica
            if (!config.validateConfiguration()) {
                logger.error("❌ Error en la configuración. Cerrando bot...");
                System.exit(1);
            }

            // Inicializar servicios
            WeatherService weatherService = new WeatherService(config);
            NewsService newsService = new NewsService(config);
            CryptoService cryptoService = new CryptoService();
            CurrencyService currencyService = new CurrencyService();

            // Crear handler de comandos
            CommandHandler commandHandler = new CommandHandler(
                    config, weatherService, newsService, cryptoService, currencyService
            );

            // Configurar y iniciar el bot
            JDA jda = JDABuilder.createDefault(config.getBotToken())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(commandHandler)
                    .build();

            // Esperar a que el bot esté completamente iniciado
            jda.awaitReady();

            logger.info("🚀 Bot iniciado correctamente!");
            logger.info("📝 Prefijo de comandos: {}", config.getBotPrefix());
            logger.info("🔗 Bot conectado como: {}", jda.getSelfUser().getAsTag());

        } catch (Exception e) {
            logger.error("💥 Error fatal al iniciar el bot: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}