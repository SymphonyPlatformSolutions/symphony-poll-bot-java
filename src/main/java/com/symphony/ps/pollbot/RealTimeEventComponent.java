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

    private final PollService pollService;

    private final MessageService messageService;

    public RealTimeEventComponent(MessageService messageService, PollService pollService) {
        this.messageService = messageService;
        this.pollService = pollService;
    }

    @EventListener
    public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
        StreamType.TypeEnum streamType = StreamType.TypeEnum.fromValue(event.getSource().getMessage().getStream().getStreamType());
        if (StreamType.TypeEnum.IM.equals(streamType) || StreamType.TypeEnum.ROOM.equals(streamType)) {
            pollService.handleIncomingMessage(event.getSource().getMessage(), streamType);
        }
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
