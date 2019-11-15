package com.symphony.ps.pollbot;

import clients.SymBotClient;
import com.sun.net.httpserver.HttpServer;
import com.symphony.ps.pollbot.listeners.ElementsListenerImpl;
import com.symphony.ps.pollbot.listeners.IMListenerImpl;
import com.symphony.ps.pollbot.listeners.RoomListenerImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.OutboundMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class PollBot {
    @Getter
    private static SymBotClient botClient;
    private static Map<Long, String> userImMap = new HashMap<>();

    public PollBot(IMListenerImpl imListener, RoomListenerImpl roomListener, ElementsListenerImpl elementsListener) {
        try {
            // Bot init
            botClient = SymBotClient.initBotRsa("config.json");

            // Bot listeners
            botClient.getDatafeedEventsService().addListeners(imListener, roomListener, elementsListener);

            // Health check endpoint
            startHealthCheckServer();

            log.info("Bot is ready.");
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
    }

    public static void sendMessage(String streamId, String message) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message));
    }

    public static void sendMessage(String streamId, String message, String data) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message, data));
    }

    public static String getImStreamId(long userId) {
        if (userImMap.containsKey(userId)) {
            return userImMap.get(userId);
        }
        String streamId = botClient.getStreamsClient().getUserIMStreamId(userId);
        userImMap.putIfAbsent(userId, streamId);
        return streamId;
    }

    private void startHealthCheckServer() {
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
            log.error("Unable to start health check: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(PollBot.class, args);
    }
}
