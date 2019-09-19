package com.symphony.ps.pollbot.services;

import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollParticipant;
import com.symphony.ps.pollbot.model.PollVote;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import model.InboundMessage;
import model.OutboundMessage;
import model.StreamTypes;
import model.User;
import model.events.SymphonyElementsAction;
import org.bson.types.ObjectId;

@Slf4j
class PollService {
    static void handleIncomingMessage(InboundMessage msg, StreamTypes streamType) {
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
                log.info("Create new poll via {} requested by {}", streamType, msg.getUser().getDisplayName());
                String pollML = MarkupService.getCreatePollML(streamType == StreamTypes.IM, options, timeLimits);
                PollBot.sendMessage(msg.getStream().getStreamId(), pollML);
                break;

            case "/endpoll":
                Poll poll = PollBot.getDataProvider().getPoll(msg.getUser().getUserId());
                if (poll == null) {
                    PollBot.sendMessage(msg.getStream().getStreamId(), "You have no active poll to end");
                    return;
                }
                handleEndPoll(poll);
                break;

            default:
        }
    }

    @SuppressWarnings("unchecked")
    static void handleCreatePoll(User initiator, SymphonyElementsAction action) {
        // Reject poll creation if an active one exists
        if (PollBot.getDataProvider().hasActivePoll(initiator.getUserId())) {
            String msg = "You already have an existing active poll. Use <b>/endpoll</b> to end it before starting a new one";
            PollBot.sendMessage(action.getStreamId(), msg);
            return;
        }

        Map<String, Object> formValues = action.getFormValues();
        log.info("New poll by {} creation in progress: {}", initiator.getDisplayName(), formValues.toString());

        // Collate options
        Map<String, String> answersMap = new HashMap<>();
        formValues.entrySet().stream()
            .filter(k -> k.getKey().startsWith("option"))
            .map(entry -> entry.getValue().toString().trim())
            .filter(answer -> !answer.isEmpty())
            .forEach(answer -> answersMap.putIfAbsent(answer.toLowerCase(), answer));
        List<String> answers = new ArrayList<>(answersMap.values());

        // Obtain IM stream IDs if audience is specified
        List<PollParticipant> participants = null;
        if (formValues.get("audience") != null) {
            participants = ((List<Long>) formValues.get("audience"))
                .parallelStream()
                .map(userId -> {
                    String streamId = PollBot.getBotClient().getStreamsClient().getUserIMStreamId(userId);
                    return new PollParticipant(userId, streamId);
                })
                .collect(Collectors.toList());
        }

        // Create poll object and persist to database
        Poll poll = Poll.builder()
            .creator(initiator.getUserId())
            .created(Instant.now())
            .questionText(formValues.get("question").toString())
            .streamId(action.getStreamId())
            .participants(participants)
            .answers(answers)
            .build();
        PollBot.getDataProvider().createPoll(poll);

        // Construct poll form and blast to audience
        String blastPollML = MarkupService.getBlastPollML(poll);
        if (formValues.get("audience") != null) {
            participants.forEach(
                participant -> PollBot.sendMessage(participant.getImStreamId(), blastPollML)
            );
        } else {
            PollBot.sendMessage(action.getStreamId(), blastPollML);
        }

        PollBot.getBotClient().getMessagesClient().sendMessage(
            action.getStreamId(), new OutboundMessage("Your poll has been created"));
        log.info("New poll by {} creation complete", initiator.getDisplayName());
    }

    static void handleSubmitVote(User initiator, SymphonyElementsAction action) {
        String answerIndexString = action.getFormValues().get("action").toString().replace("option-", "");
        int answerIndex = Integer.parseInt(answerIndexString);

        String pollId = action.getFormId().replace("poll-blast-form-", "");
        Poll poll = PollBot.getDataProvider().getPoll(pollId);

        if (poll == null) {
            PollBot.sendMessage(action.getStreamId(), "Unable to find matching poll to submit vote for");
            return;
        }
        if (poll.getEnded() != null) {
            PollBot.sendMessage(action.getStreamId(), "This poll is no longer active for voting");
            return;
        }
        String answer = poll.getAnswers().get(answerIndex);

        String response;
        if (PollBot.getDataProvider().hasVoted(initiator.getUserId(), pollId)) {
            PollBot.getDataProvider().changeVote(initiator.getUserId(), pollId, answer);
            response = "Your vote has been updated";
        } else {
            PollVote vote = PollVote.builder()
                .pollId(new ObjectId(pollId))
                .answer(answer)
                .userId(initiator.getUserId())
                .build();
            PollBot.getDataProvider().createVote(vote);
            response = "Thanks for voting";
        }

        PollBot.sendMessage(action.getStreamId(),
            String.format("%s, <mention uid=\"%d\" />!", response, initiator.getUserId()));
        log.info("Vote [{}] for poll {} by {} captured", answer, pollId, initiator.getDisplayName());
    }

    static void handleEndPoll(Poll poll) {
        List<PollVote> votes = PollBot.getDataProvider().getVotes(poll.getId());
        String response;

        if (votes.isEmpty()) {
            response = "Poll ended but with no results to show";
        } else {
            // Aggregate vote results
            Map<String, Long> voteResultsMap = votes.stream()
                .map(PollVote::getAnswer)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            // Add in 0 votes for options nobody voted on
            poll.getAnswers().stream()
                .filter(answer -> !voteResultsMap.containsKey(answer))
                .forEach(answer -> voteResultsMap.put(answer, 0L));

            // Construct results table
            long maxVal = Collections.max(voteResultsMap.values());
            String resultsRowTemplate = "<tr><td>%s</td><td style=\"text-align:right\">%d</td><td><div style='background-color:#29b6f6;width:%dpx'> </div></td></tr>";
            String resultsRowsML = voteResultsMap.entrySet().stream().map(e -> {
                int width = (int) (((float) e.getValue() / maxVal) * 200);
                if (width == 0 && e.getValue() > 0) {
                    width = 1;
                }
                return String.format(resultsRowTemplate, e.getKey(), e.getValue(), width);
            }).collect(Collectors.joining(""));

            response = String.format(
                "<h5>Results for Poll: %s</h5><table><tr><th>Option</th><th style=\"text-align:right\">Votes</th><th></th></tr>%s</table>",
                poll.getQuestionText(), resultsRowsML
            );
        }

        PollBot.getDataProvider().endPoll(poll.getCreator());
        PollBot.sendMessage(poll.getStreamId(), response);
    }
}
