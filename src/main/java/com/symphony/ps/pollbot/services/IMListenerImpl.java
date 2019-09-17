package com.symphony.ps.pollbot.services;

import com.mongodb.client.MongoCollection;
import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.model.Poll;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import listeners.IMListener;
import lombok.extern.slf4j.Slf4j;
import model.InboundMessage;
import model.Stream;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@Slf4j
public class IMListenerImpl implements IMListener {
    private MongoCollection<Poll> pollCollection;

    public IMListenerImpl() {
        this.pollCollection = PollBot.getDataProvider().getPollCollection();
    }

    public void onIMMessage(InboundMessage msg) {
        int options = 6;
        List<Integer> timeLimits = Arrays.asList(0, 2, 5);

        String msgText = msg.getMessageText().trim().toLowerCase();
        String[] msgParts = msgText.split(" ", 2);

        if (msgParts.length > 1) {
            for (String pollConfig : msgParts[1].split(" ")) {
                if (pollConfig.matches("^\\d+$")) {
                    options = Integer.parseInt(pollConfig);
                    if (options < 2 || options > 10) {
                        PollBot.sendMessage(msg.getStream().getStreamId(), "Number of options must be between 2 and 10");
                        return;
                    }
                } else if (pollConfig.matches("^\\d+(,\\d)+$")) {
                    timeLimits = Arrays.stream(pollConfig.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                    if (timeLimits.size() > 10) {
                        PollBot.sendMessage(msg.getStream().getStreamId(), "Number of time limits should be 10 or lower");
                        return;
                    } else if (timeLimits.stream().anyMatch(t -> t < 0)) {
                        PollBot.sendMessage(msg.getStream().getStreamId(), "Time limits contains negative numbers");
                        return;
                    }
                }
            }
        }

        switch (msgParts[0]) {
            case "/poll":
                log.info("Create new poll via IM requested by {}", msg.getUser().getDisplayName());
                String pollML = PollService.getPollML(true, options, timeLimits);
                PollBot.sendMessage(msg.getStream().getStreamId(), pollML);
                break;

            case "/endpoll":
                pollCollection.updateOne(and(
                    eq("creator", msg.getUser().getUserId()),
                    eq("ended", null)
                ), set("ended", Instant.now()));
                PollBot.sendMessage(msg.getStream().getStreamId(), "Your poll has been ended");
                break;

            default:
        }
    }

    public void onIMCreated(Stream stream) {}
}
