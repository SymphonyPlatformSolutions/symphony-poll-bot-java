package com.symphony.ps.pollbot.listeners;

import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.services.PollService;
import listeners.ElementsListener;
import lombok.extern.slf4j.Slf4j;
import model.User;
import model.events.SymphonyElementsAction;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ElementsListenerImpl implements ElementsListener {
    private final PollService pollService;

    public ElementsListenerImpl(PollService pollService) {
        this.pollService = pollService;
    }

    public void onElementsAction(User initiator, SymphonyElementsAction action) {
        String formId = action.getFormId();
        if (formId.equals("poll-create-form")) {
            pollService.handleCreatePoll(initiator, action);
        } else if (formId.matches("poll\\-blast\\-form\\-[\\w\\d]+")) {
            pollService.handleSubmitVote(initiator, action);
        } else {
            PollBot.sendMessage(action.getStreamId(), "Sorry, I do not understand this form submission");
        }
    }
}
