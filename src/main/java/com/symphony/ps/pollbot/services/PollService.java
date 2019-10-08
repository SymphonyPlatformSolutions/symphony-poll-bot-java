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
                log.info("Get new poll form via {} requested by {}", streamType, msg.getUser().getDisplayName());

                String pollML = MarkupService.newPollTemplate;
                String data = MarkupService.getNewPollData(streamType == StreamTypes.IM, options, timeLimits);
                PollBot.sendMessage(msg.getStream().getStreamId(), pollML, data);

                log.info("New poll form sent to {} stream {}", streamType, msg.getStream().getStreamId());
                break;

            case "/endpoll":
                log.info("End poll requested by {}", msg.getUser().getDisplayName());
                Poll poll = PollBot.getDataProvider().getPoll(msg.getUser().getUserId());
                if (poll == null) {
                    PollBot.sendMessage(msg.getStream().getStreamId(), "You have no active poll to end");
                    log.info("User {} has no active poll to end", msg.getUser().getDisplayName());
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
            String msg = String.format("<mention uid=\"%d\"/> You already have an existing active poll." +
                "Use <b>/endpoll</b> to end it before starting a new one", initiator.getUserId());
            PollBot.sendMessage(action.getStreamId(), msg);
            log.info("User {} has an existing active poll. Refusing to create a new one.", initiator.getDisplayName());
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

        if (answers.size() < 2) {
            String rejectMsg = String.format("<mention uid=\"%d\"/> Your poll contains less than 2 valid options",
                initiator.getUserId());
            PollBot.sendMessage(action.getStreamId(), rejectMsg);
            log.info("Create poll by {} rejected as there are less than 2 valid options", initiator.getDisplayName());
            return;
        }

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

        int timeLimit = Integer.parseInt(formValues.get("timeLimit").toString());

        // Create poll object and persist to database
        Poll poll = Poll.builder()
            .creator(initiator.getUserId())
            .created(Instant.now())
            .timeLimit(timeLimit)
            .questionText(formValues.get("question").toString())
            .streamId(action.getStreamId())
            .participants(participants)
            .answers(answers)
            .build();
        PollBot.getDataProvider().createPoll(poll);

        // Construct poll form and blast to audience
        String blastPollML = MarkupService.getBlastPollML(poll);
        if (participants != null) {
            participants.forEach(
                participant -> PollBot.sendMessage(participant.getImStreamId(), blastPollML)
            );
        } else {
            PollBot.sendMessage(action.getStreamId(), blastPollML);
        }

        // Start timer
        if (timeLimit > 0) {
            Timer timer = new Timer("PollTimer" + poll.getId());
            timer.schedule(new TimerTask() {
                public void run() {
                    handleEndPoll(poll);
                }
            }, 60000L * timeLimit);
        }

        PollBot.getBotClient().getMessagesClient().sendMessage(action.getStreamId(),
            new OutboundMessage(String.format("<mention uid=\"%d\"/> Your poll has been created", initiator.getUserId())));
        log.info("New poll by {} creation complete", initiator.getDisplayName());
    }

    static void handleSubmitVote(User initiator, SymphonyElementsAction action) {
        String answerIndexString = action.getFormValues().get("action").toString().replace("option-", "");
        int answerIndex = Integer.parseInt(answerIndexString);

        String pollId = action.getFormId().replace("poll-blast-form-", "");
        Poll poll = PollBot.getDataProvider().getPoll(pollId);

        if (poll == null) {
            PollBot.sendMessage(action.getStreamId(), String.format(
                "<mention uid=\"%d\"/> You have submitted a vote for an invalid poll", initiator.getUserId()));
            log.info("Invalid vote cast by {} on {} stream {}",
                initiator.getDisplayName(), action.getStreamType(), action.getStreamId());
            return;
        }
        if (poll.getEnded() != null) {
            PollBot.sendMessage(action.getStreamId(), String.format(
                "<mention uid=\"%d\"/> This poll is no longer active for voting", initiator.getUserId()));
            log.info("Vote cast on expired poll by {} on {} stream {}",
                initiator.getDisplayName(), action.getStreamType(), action.getStreamId());
            return;
        }
        String answer = poll.getAnswers().get(answerIndex);

        String response;
        if (PollBot.getDataProvider().hasVoted(initiator.getUserId(), pollId)) {
            PollBot.getDataProvider().changeVote(initiator.getUserId(), pollId, answer);
            response = "Your vote has been updated";
            log.info("Vote updated to [{}] on poll {} by {}", answer, poll.getId(), initiator.getDisplayName());
        } else {
            PollVote vote = PollVote.builder()
                .pollId(new ObjectId(pollId))
                .answer(answer)
                .userId(initiator.getUserId())
                .build();
            PollBot.getDataProvider().createVote(vote);
            response = "Thanks for voting";
            log.info("New vote [{}] cast on poll {} by {}", answer, poll.getId(), initiator.getDisplayName());
        }

        PollBot.sendMessage(action.getStreamId(),
            String.format("<mention uid=\"%d\"/> %s!", initiator.getUserId(), response));
    }

    private static void handleEndPoll(Poll poll) {
        List<PollVote> votes = PollBot.getDataProvider().getVotes(poll.getId());
        String response;

        if (votes.isEmpty()) {
            response = "Poll ended but with no results to show";
            log.info("Poll {} ended with no votes", poll.getId());
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

            log.info("Poll {} ended with results {}", poll.getId(), voteResultsMap.toString());
        }

        PollBot.getDataProvider().endPoll(poll.getCreator());
        PollBot.sendMessage(poll.getStreamId(), response);
    }
}
