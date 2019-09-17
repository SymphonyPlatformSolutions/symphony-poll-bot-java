package com.symphony.ps.pollbot.services;

import clients.SymBotClient;
import com.mongodb.client.MongoCollection;
import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollAnswer;
import com.symphony.ps.pollbot.model.PollParticipant;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import listeners.ElementsListener;
import lombok.extern.slf4j.Slf4j;
import model.OutboundMessage;
import model.StreamTypes;
import model.User;
import model.events.SymphonyElementsAction;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class ElementsListenerImpl implements ElementsListener {
    private SymBotClient botClient;
    private MongoCollection<Poll> pollCollection;

    public ElementsListenerImpl() {
        this.botClient = PollBot.getBotClient();
        this.pollCollection = PollBot.getDataProvider().getPollCollection();
    }

    @SuppressWarnings("unchecked")
    public void onElementsAction(User initiator, SymphonyElementsAction action) {
        long activePolls = pollCollection.countDocuments(
            and(
                eq("creator", initiator.getUserId()),
                eq("ended", null)
            )
        );

        if (activePolls > 0L) {
            String msg = "You already have an existing active poll. Use <b>/endpoll</b> to end it before starting a new one";
            PollBot.sendMessage(action.getStreamId(), msg);
            return;
        }

        Map<String, Object> formValues = action.getFormValues();
        log.info("New poll by {} creation in progress: {}", initiator.getDisplayName(), formValues.toString());

        List<PollAnswer> answers = formValues.entrySet().stream()
            .filter(k -> k.getKey().startsWith("option"))
            .map(entry -> new PollAnswer(entry.getValue().toString()))
            .collect(Collectors.toList());

        List<PollParticipant> participants = null;

        if (formValues.get("audience") != null) {
            participants = ((List<Long>) formValues.get("audience"))
                .parallelStream()
                .map(userId -> {
                    String streamId = botClient.getStreamsClient().getUserIMStreamId(userId);
                    return new PollParticipant(userId, streamId);
                })
                .collect(Collectors.toList());
        }

        Poll poll = Poll.builder()
            .creator(initiator.getUserId())
            .created(Instant.now())
            .questionText(formValues.get("question").toString())
            .type(participants == null ? StreamTypes.ROOM : StreamTypes.IM)
            .participants(participants)
            .answers(answers)
            .build();
        pollCollection.insertOne(poll);

        // TODO: construct poll message

        // TODO: blast poll message to audience

        botClient.getMessagesClient().sendMessage(action.getStreamId(), new OutboundMessage("Your poll has been created"));
        log.info("New poll by {} creation complete", initiator.getDisplayName());
    }
}
