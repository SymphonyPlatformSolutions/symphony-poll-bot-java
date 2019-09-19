package com.symphony.ps.pollbot;

import clients.SymBotClient;
import com.symphony.ps.pollbot.data.DataProvider;
import com.symphony.ps.pollbot.model.PollBotConfig;
import com.symphony.ps.pollbot.services.ElementsListenerImpl;
import com.symphony.ps.pollbot.services.IMListenerImpl;
import com.symphony.ps.pollbot.services.RoomListenerImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.OutboundMessage;
import org.apache.log4j.*;

@Slf4j
public class PollBot {
    @Getter
    private static SymBotClient botClient;
    @Getter
    private static DataProvider dataProvider;

    public static void main(String[] args) {
        // Logging
        Logger root = Logger.getRootLogger();
        String logPattern = "%d{yyyy-MM-dd HH:mm:ss} [%p].(%F:%L) %m%n";
        root.addAppender(new ConsoleAppender(new PatternLayout(logPattern)));
        root.setLevel(Level.INFO);
        LogManager.getLogger("org.mongodb.driver").setLevel(Level.ERROR);

        // Bot init
        botClient = SymBotClient.initBotRsa("config.json", PollBotConfig.class);

        // Set up MongoDB
        dataProvider = new DataProvider(botClient.getConfig(PollBotConfig.class).getMongoUri());

        // Bot listeners
        botClient.getDatafeedEventsService().addListeners(
            new IMListenerImpl(),
            new RoomListenerImpl(),
            new ElementsListenerImpl()
        );

        log.info("Bot is ready.");
    }

    public static void sendMessage(String streamId, String message) {
        botClient.getMessagesClient().sendMessage(streamId, new OutboundMessage(message));
    }
}
