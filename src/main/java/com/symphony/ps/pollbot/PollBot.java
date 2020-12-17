package com.symphony.ps.pollbot;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
@SpringBootApplication
public class PollBot {

    public PollBot() {
        try {
            // Health check endpoint
            startHealthCheckServer();

            log.info("Bot is ready.");
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
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
