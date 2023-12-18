package com.symphony.devrel.pollbot.service;

import com.symphony.bdk.core.service.health.HealthService;
import com.symphony.bdk.gen.api.model.V3HealthStatus;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
@Component
@Profile("prod")
public class HealthzService {
    private HealthzService(HealthService health) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/healthz", exchange -> {
                V3HealthStatus status = health.datafeedHealthCheck();
                String response = "{ \"status\": \"" + status + "\" }";
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
