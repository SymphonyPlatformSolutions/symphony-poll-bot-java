package com.symphony.ps.pollbot;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.*;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.ps.pollbot.services.PollService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RealTimeEventComponent {

    @Autowired
    private PollService pollService;

    @Autowired
    private MessageService messageService;

    @EventListener
    public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
        pollService.handleIncomingMessage(
                event.getSource().getMessage(),
                StreamType.TypeEnum.fromValue(event.getSource().getMessage().getStream().getStreamType()));
    }

    @EventListener
    public void onSymphonyElementAction(RealTimeEvent<V4SymphonyElementsAction> event) {
        V4SymphonyElementsAction action = event.getSource();
        V4User initiator = event.getInitiator().getUser();
        String formId = action.getFormId();
        if (formId.equals("poll-create-form")) {
            pollService.handleCreatePoll(initiator, action);
        } else if (formId.matches("poll\\-blast\\-form\\-[\\w\\d]+")) {
            pollService.handleSubmitVote(initiator, action);
        } else {
            messageService.send(action.getStream().getStreamId(), "Sorry, I do not understand this form submission");
        }
    }

}
