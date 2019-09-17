package com.symphony.ps.pollbot;

import clients.SymBotClient;
import com.symphony.ps.pollbot.data.DataProvider;
import com.symphony.ps.pollbot.services.ElementsListenerImpl;
import com.symphony.ps.pollbot.services.IMListenerImpl;
import com.symphony.ps.pollbot.model.PollBotConfig;
import lombok.Getter;
import model.OutboundMessage;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

public class PollBot {
    @Getter
    private static SymBotClient botClient;
    @Getter
    private static DataProvider dataProvider;

    public static void main(String[] args) {
        // Logging
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        LogManager.getLogger("org.mongodb.driver").setLevel(Level.ERROR);

        // Bot init
        botClient = SymBotClient.initBotRsa("config.json", PollBotConfig.class);

        // Set up MongoDB
        dataProvider = new DataProvider(botClient.getConfig(PollBotConfig.class).getMongoUri());

        // Bot listeners
        botClient.getDatafeedEventsService().addListeners(
            new IMListenerImpl(),
            new ElementsListenerImpl()
        );
    }

    public static void sendMessage(String streamId, String message) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message));
    }
}
