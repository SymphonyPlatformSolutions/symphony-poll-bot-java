package com.symphony.ps.pollbot.services;

import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.data.DataProvider;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import listeners.IMListener;
import lombok.extern.slf4j.Slf4j;
import model.InboundMessage;
import model.Stream;

@Slf4j
public class IMListenerImpl implements IMListener {
    private DataProvider data;

    public IMListenerImpl() {
        this.data = PollBot.getDataProvider();
    }

    public void onIMMessage(InboundMessage msg) {
        int options = 6;
        List<Integer> timeLimits = Arrays.asList(0, 2, 5);

        String streamId = msg.getStream().getStreamId();
        String msgText = msg.getMessageText().trim().toLowerCase();
        String[] msgParts = msgText.split(" ", 2);

        if (msgParts.length > 1) {
            for (String pollConfig : msgParts[1].split(" ")) {
                if (pollConfig.matches("^\\d+$")) {
                    options = Integer.parseInt(pollConfig);
                    if (options < 2 || options > 10) {
                        PollBot.sendMessage(streamId, "Number of options must be between 2 and 10");
                        return;
                    }
                } else if (pollConfig.matches("^\\d+(,\\d)+$")) {
                    timeLimits = Arrays.stream(pollConfig.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                    if (timeLimits.size() > 10) {
                        PollBot.sendMessage(streamId, "Number of time limits should be 10 or lower");
                        return;
                    } else if (timeLimits.stream().anyMatch(t -> t < 0)) {
                        PollBot.sendMessage(streamId, "Time limits contains negative numbers");
                        return;
                    }
                }
            }
        }

        switch (msgParts[0]) {
            case "/poll":
                log.info("Create new poll via IM requested by {}", msg.getUser().getDisplayName());
                String pollML = PollService.getCreatePollML(true, options, timeLimits);
                PollBot.sendMessage(msg.getStream().getStreamId(), pollML);
                break;

            case "/endpoll":
                if (!data.hasActivePoll(msg.getUser().getUserId())) {
                    PollBot.sendMessage(msg.getStream().getStreamId(), "You have no active poll to end");
                    return;
                }
                data.endPoll(msg.getUser().getUserId());
                PollBot.sendMessage(msg.getStream().getStreamId(), "Your active poll has been ended");
                break;

            default:
        }
    }

    public void onIMCreated(Stream stream) {}
}
