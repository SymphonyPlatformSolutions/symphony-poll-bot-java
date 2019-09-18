package com.symphony.ps.pollbot.services;

import clients.SymBotClient;
import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.data.DataProvider;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollParticipant;
import com.symphony.ps.pollbot.model.PollVote;
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
import org.bson.types.ObjectId;

@Slf4j
public class ElementsListenerImpl implements ElementsListener {
    private SymBotClient botClient;
    private DataProvider data;

    public ElementsListenerImpl() {
        this.botClient = PollBot.getBotClient();
        this.data = PollBot.getDataProvider();
    }

    public void onElementsAction(User initiator, SymphonyElementsAction action) {
        String formId = action.getFormId();
        if (formId.equals("poll-create-form")) {
            handleCreatePoll(initiator, action);
        } else if (formId.matches("poll\\-blast\\-form\\-[\\w\\d]+")) {
            handleSubmitPoll(initiator, action);
        } else {
            PollBot.sendMessage(action.getStreamId(), "Sorry, I do not understand this form submission");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCreatePoll(User initiator, SymphonyElementsAction action) {
        // Reject poll creation if an active one exists
        if (data.hasActivePoll(initiator.getUserId())) {
            String msg = "You already have an existing active poll. Use <b>/endpoll</b> to end it before starting a new one";
            PollBot.sendMessage(action.getStreamId(), msg);
            return;
        }

        Map<String, Object> formValues = action.getFormValues();
        log.info("New poll by {} creation in progress: {}", initiator.getDisplayName(), formValues.toString());

        // Collate options
        List<String> answers = formValues.entrySet().stream()
            .filter(k -> k.getKey().startsWith("option"))
            .map(entry -> entry.getValue().toString())
            .collect(Collectors.toList());

        // Obtain IM stream IDs if audience is specified
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

        // Create poll object and persist to database
        Poll poll = Poll.builder()
            .creator(initiator.getUserId())
            .created(Instant.now())
            .questionText(formValues.get("question").toString())
            .type(participants == null ? StreamTypes.ROOM : StreamTypes.IM)
            .participants(participants)
            .answers(answers)
            .build();
        data.createPoll(poll);

        // Construct poll form and blast to audience
        String blastPollML = PollService.getBlastPollML(poll);
        if (formValues.get("audience") != null) {
            participants.forEach(
                participant -> PollBot.sendMessage(participant.getImStreamId(), blastPollML)
            );
        } else {
            PollBot.sendMessage(action.getStreamId(), blastPollML);
        }

        botClient.getMessagesClient().sendMessage(action.getStreamId(), new OutboundMessage("Your poll has been created"));
        log.info("New poll by {} creation complete", initiator.getDisplayName());
    }

    private void handleSubmitPoll(User initiator, SymphonyElementsAction action) {
        String answerIndexString = action.getFormValues().get("action").toString().replace("option-", "");
        int answerIndex = Integer.parseInt(answerIndexString);

        String pollId = action.getFormId().replace("poll-blast-form-", "");
        Poll poll = data.getPoll(pollId);

        if (poll == null) {
            PollBot.sendMessage(action.getStreamId(), "Unable to find matching poll to submit vote for");
            return;
        }
        String answer = poll.getAnswers().get(answerIndex);

        String response;
        if (data.hasVoted(initiator.getUserId(), pollId)) {
            data.changeVote(initiator.getUserId(), pollId, answer);
            response = "Your vote has been updated";
        } else {
            PollVote vote = PollVote.builder()
                .pollId(new ObjectId(pollId))
                .answer(answer)
                .userId(initiator.getUserId())
                .build();
            data.createVote(vote);
            response = "Thanks for voting";
        }

        PollBot.sendMessage(action.getStreamId(),
            String.format("%s, <mention uid=\"%d\" />!", response, initiator.getUserId()));
        log.info("Vote [{}] for poll {} by {} captured", answer, pollId, initiator.getDisplayName());
    }
}
