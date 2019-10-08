package com.symphony.ps.pollbot.listeners;

import com.symphony.ps.pollbot.services.PollService;
import listeners.IMListener;
import lombok.extern.slf4j.Slf4j;
import model.InboundMessage;
import model.Stream;
import model.StreamTypes;

@Slf4j
public class IMListenerImpl implements IMListener {
    public void onIMMessage(InboundMessage msg) {
        PollService.handleIncomingMessage(msg, StreamTypes.IM);
    }

    public void onIMCreated(Stream stream) {}
}
