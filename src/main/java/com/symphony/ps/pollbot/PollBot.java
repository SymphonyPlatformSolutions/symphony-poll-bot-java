package com.symphony.ps.pollbot;

import clients.SymBotClient;
import com.sun.net.httpserver.HttpServer;
import com.symphony.ps.pollbot.listeners.ElementsListenerImpl;
import com.symphony.ps.pollbot.listeners.IMListenerImpl;
import com.symphony.ps.pollbot.listeners.RoomListenerImpl;
import com.symphony.ps.pollbot.model.PollBotConfig;
import com.symphony.ps.pollbot.services.DataService;
import java.io.IOException;
import java.net.InetSocketAddress;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.OutboundMessage;
import org.apache.log4j.*;

@Slf4j
public class PollBot {
    @Getter
    private static SymBotClient botClient;
    @Getter
    private static DataService dataService;

    public static void main(String[] args) {
        // Logging
        Logger root = Logger.getRootLogger();
        String logPattern = "%d{yyyy-MM-dd HH:mm:ss} [%p].(%F:%L) %m%n";
        root.addAppender(new ConsoleAppender(new PatternLayout(logPattern)));
        root.setLevel(Level.INFO);
        LogManager.getLogger("org.mongodb.driver").setLevel(Level.ERROR);

        try {
            // Bot init
            botClient = SymBotClient.initBotRsa("config.json", PollBotConfig.class);

            // Set up MongoDB
            dataService = new DataService(botClient.getConfig(PollBotConfig.class).getMongoUri());

            // Bot listeners
            botClient.getDatafeedEventsService().addListeners(
                new IMListenerImpl(),
                new RoomListenerImpl(),
                new ElementsListenerImpl()
            );

            startHealthCheckServer();
            log.info("Bot is ready.");
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public static void sendMessage(String streamId, String message) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message));
    }

    public static void sendMessage(String streamId, String message, String data) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message, data));
    }

    private static void startHealthCheckServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/healthz", exchange -> {
                String response = "{ \"status\": \"UP\" }";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            });
            server.setExecutor(null);
            server.start();
            log.info("Health check endpoint is up");
        } catch (IOException e) {
            log.error("Unable to start health check", e);
        }
    }
}
