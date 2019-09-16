package com.symphony.ps.pollbot.listeners;

import clients.SymBotClient;
import com.symphony.ps.pollbot.PollBot;
import java.util.Map;
import listeners.ElementsListener;
import model.OutboundMessage;
import model.User;
import model.events.SymphonyElementsAction;

public class ElementsListenerImpl implements ElementsListener {
    private SymBotClient botClient;

    public ElementsListenerImpl() {
        this.botClient = PollBot.getBotClient();
    }

    public void onElementsAction(User initiator, SymphonyElementsAction action) {
        Map<String, Object> formValues = action.getFormValues();
        botClient.getMessagesClient().sendMessage(action.getStreamId(), new OutboundMessage("Your action has been processed"));
        botClient.getMessagesClient().sendMessage(action.getStreamId(), new OutboundMessage(formValues.toString()));
    }
}
