package com.symphony.ps.pollbot.listeners;

import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.services.PollService;
import listeners.ElementsListener;
import lombok.extern.slf4j.Slf4j;
import model.User;
import model.events.SymphonyElementsAction;

@Slf4j
public class ElementsListenerImpl implements ElementsListener {
    public void onElementsAction(User initiator, SymphonyElementsAction action) {
        String formId = action.getFormId();
        if (formId.equals("poll-create-form")) {
            PollService.handleCreatePoll(initiator, action);
        } else if (formId.matches("poll\\-blast\\-form\\-[\\w\\d]+")) {
            PollService.handleSubmitVote(initiator, action);
        } else {
            PollBot.sendMessage(action.getStreamId(), "Sorry, I do not understand this form submission");
        }
    }
}
