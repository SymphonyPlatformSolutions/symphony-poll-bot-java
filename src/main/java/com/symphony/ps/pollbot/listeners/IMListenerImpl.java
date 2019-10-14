package com.symphony.ps.pollbot.listeners;

import com.symphony.ps.pollbot.services.PollService;
import listeners.IMListener;
import lombok.extern.slf4j.Slf4j;
import model.InboundMessage;
import model.Stream;
import model.StreamTypes;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IMListenerImpl implements IMListener {
    private final PollService pollService;

    public IMListenerImpl(PollService pollService) {
        this.pollService = pollService;
    }

    public void onIMMessage(InboundMessage msg) {
        pollService.handleIncomingMessage(msg, StreamTypes.IM);
    }

    public void onIMCreated(Stream stream) {}
}
