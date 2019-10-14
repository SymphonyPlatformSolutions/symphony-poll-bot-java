package com.symphony.ps.pollbot;

import clients.SymBotClient;
import com.symphony.ps.pollbot.listeners.ElementsListenerImpl;
import com.symphony.ps.pollbot.listeners.IMListenerImpl;
import com.symphony.ps.pollbot.listeners.RoomListenerImpl;
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

    public PollBot(IMListenerImpl imListener, RoomListenerImpl roomListener, ElementsListenerImpl elementsListener) {
        try {
            // Bot init
            botClient = SymBotClient.initBotRsa("config.json");

            // Bot listeners
            botClient.getDatafeedEventsService().addListeners(
                imListener, roomListener, elementsListener
            );

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

    public static void main(String[] args) {
        SpringApplication.run(PollBot.class, args);
    }
}
