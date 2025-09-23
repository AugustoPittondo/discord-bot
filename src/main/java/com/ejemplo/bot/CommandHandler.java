package com.ejemplo.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manejador principal de comandos del bot
 * Procesa todos los mensajes y ejecuta los comandos correspondientes
 */
public class CommandHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    private final ConfigManager config;
    private final WeatherService weatherService;
    private final NewsService newsService;
    private final CryptoService cryptoService;
    private final CurrencyService currencyService;

    public CommandHandler(ConfigManager config, WeatherService weatherService,
                          NewsService newsService, CryptoService cryptoService,
                          CurrencyService currencyService) {
        this.config = config;
        this.weatherService = weatherService;
        this.newsService = newsService;
        this.cryptoService = cryptoService;
        this.currencyService = currencyService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignorar mensajes de bots
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw().trim();
        String prefix = config.getBotPrefix();

        // Verificar si es un comando
        if (!message.startsWith(prefix)) return;

        // Extraer comando y argumentos
        String[] parts = message.substring(prefix.length()).split("\\s+");
        String command = parts[0].toLowerCase();

        // Log del comando ejecutado
        logger.info("🎮 Comando ejecutado: {} por {}",
                message, event.getAuthor().getAsTag());

        // Mostrar typing mientras se procesa
        event.getChannel().sendTyping().queue();

        try {
            switch (command) {
                case "ping":
                    handlePingCommand(event);
                    break;
                case "ayuda":
                case "help":
                    handleHelpCommand(event);
                    break;
                case "info":
                    handleInfoCommand(event);
                    break;
                case "hora":
                case "tiempo":
                    handleTimeCommand(event);
                    break;
                case "dolar":
                case "usd":
                    handleDolarCommand(event);
                    break;
                case "crypto":
                case "bitcoin":
                    handleCryptoCommand(event);
                    break;
                case "clima":
                    handleWeatherCommand(event, parts);
                    break;
                case "noticias":
                case "news":
                    handleNewsCommand(event);
                    break;
                default:
                    handleUnknownCommand(event, command);
            }
        } catch (Exception e) {
            logger.error("💥 Error procesando comando {}: {}", command, e.getMessage());
            event.getChannel().sendMessage("❌ Error interno del bot. Intenta nuevamente.").queue();
        }
    }

    private void handlePingCommand(MessageReceivedEvent event) {
        event.getChannel().sendMessage("🏓 Pong! Bot funcionando correctamente.").queue();
    }

    private void handleHelpCommand(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Lista de Comandos Disponibles");
        embed.setColor(Color.BLUE);
        embed.setDescription("Aquí tienes todos los comandos que puedes usar:");

        embed.addField("🏓 Básicos",
                "`!ping` - Verificar si el bot funciona\n" +
                        "`!info` - Información sobre el bot\n" +
                        "`!hora` / `!tiempo` - Fecha y hora actual", false);

        embed.addField("💰 Finanzas",
                "`!dolar` / `!usd` - Cotización del dólar\n" +
                        "`!crypto` / `!bitcoin` - Precios de criptomonedas", false);

        embed.addField("🌍 Servicios",
                "`!clima [ciudad]` - Información del clima\n" +
                        "`!noticias` / `!news` - Últimas noticias", false);

        embed.addField("ℹ️ Ayuda",
                "`!ayuda` / `!help` - Mostrar esta ayuda", false);

        embed.setFooter("Prefijo: " + config.getBotPrefix());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleInfoCommand(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🤖 Información del Bot");
        embed.setColor(Color.GREEN);
        embed.addField("📊 Estado", "✅ Online y funcionando", true);
        embed.addField("🔧 Versión", "1.0.0", true);
        embed.addField("👥 Servidores", String.valueOf(event.getJDA().getGuilds().size()), true);
        embed.addField("⚡ Prefijo", config.getBotPrefix(), true);
        embed.setFooter("Desarrollado por el equipo");

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleTimeCommand(MessageReceivedEvent event) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🕐 Fecha y Hora Actual");
        embed.setDescription("**" + now.format(formatter) + "**");
        embed.setColor(Color.CYAN);

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleDolarCommand(MessageReceivedEvent event) {
        try {
            String result = currencyService.getDollarRate();
            event.getChannel().sendMessage(result).queue();
        } catch (Exception e) {
            logger.error("Error obteniendo cotización del dólar: {}", e.getMessage());
            event.getChannel().sendMessage("❌ Error al obtener la cotización del dólar.").queue();
        }
    }

    private void handleCryptoCommand(MessageReceivedEvent event) {
        try {
            String result = cryptoService.getCryptoPrices();
            event.getChannel().sendMessage(result).queue();
        } catch (Exception e) {
            logger.error("Error obteniendo precios de criptomonedas: {}", e.getMessage());
            event.getChannel().sendMessage("❌ Error al obtener precios de criptomonedas.").queue();
        }
    }

    private void handleWeatherCommand(MessageReceivedEvent event, String[] parts) {
        if (parts.length < 2) {
            event.getChannel().sendMessage("❌ Debes especificar una ciudad. Ejemplo: `!clima Buenos Aires`").queue();
            return;
        }

        String city = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));

        try {
            String result = weatherService.getWeather(city);
            event.getChannel().sendMessage(result).queue();
        } catch (Exception e) {
            logger.error("Error obteniendo clima para {}: {}", city, e.getMessage());
            event.getChannel().sendMessage("❌ Error al obtener información del clima para: " + city).queue();
        }
    }

    private void handleNewsCommand(MessageReceivedEvent event) {
        try {
            String result = newsService.getLatestNews();
            event.getChannel().sendMessage(result).queue();
        } catch (Exception e) {
            logger.error("Error obteniendo noticias: {}", e.getMessage());
            event.getChannel().sendMessage("❌ Error al obtener las últimas noticias.").queue();
        }
    }

    private void handleUnknownCommand(MessageReceivedEvent event, String command) {
        event.getChannel().sendMessage(
                "❓ Comando `" + command + "` no reconocido. " +
                        "Usa `" + config.getBotPrefix() + "ayuda` para ver todos los comandos."
        ).queue();
    }
}

