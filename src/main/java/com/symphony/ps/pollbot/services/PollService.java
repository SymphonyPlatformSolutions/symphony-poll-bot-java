package com.symphony.ps.pollbot.services;

import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.model.*;
import exceptions.ForbiddenException;
import exceptions.SymClientException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import model.*;
import model.events.SymphonyElementsAction;
import org.springframework.stereotype.Service;
import utils.MessageUtils;

@Slf4j
@Service
public class PollService {
    private final DataService dataService;

    public PollService(DataService dataService) {
        this.dataService = dataService;
    }

    private static final String helpML = "<ul>" +
        "<li><b>/poll</b>: Get a standard create poll form</li>" +
        "<li><b>/poll room</b>: Get a create poll form that targets a room via stream id</li>" +
        "<li><b>/poll 4 0,5,15</b>: Get a create poll form with 4 options and time limits of None, 5 and 15 minutes</li>" +
        "<li><b>/endpoll</b>: End your active poll</li>" +
        "<li><b>/history</b>: View your personal poll history</li></ul>";

    public void handleIncomingMessage(InboundMessage msg, StreamTypes streamType) {
        long userId = msg.getUser().getUserId();
        String displayName = msg.getUser().getDisplayName();
        String streamId = msg.getStream().getStreamId();
        String msgText = msg.getMessageText();
        if (msgText == null) {
            log.info("Ignoring message as message text cannot be parsed");
            return;
        }
        String[] msgParts = msgText.trim().toLowerCase().split(" ", 2);

        switch (msgParts[0]) {
            case "/help":
                String helpMLForStream = streamType == StreamTypes.IM ? helpML
                    : helpML.replace("</li></ul>", " for this room</li></ul>");
                PollBot.sendMessage(streamId, helpMLForStream);
                break;

            case "/poll":
            case "/createpoll":
                PollConfig pollConfig = (msgParts.length == 1) ? new PollConfig()
                    : parseConfigInput(streamId, msgParts[1].split(" "));

                if (pollConfig == null) {
                    return;
                }
                handleSendCreatePollForm(streamId, streamType, msg.getUser(), pollConfig);
                break;

            case "/rigpoll":
                handleRigPoll(streamId, userId, displayName);
                break;

            case "/endpoll":
                handleEndPoll(streamId, userId, displayName);
                break;

            case "/history":
                handleHistory(streamId, streamType, userId, displayName);
                break;

            default:
        }
    }

    private static PollConfig parseConfigInput(String streamId, String[] inputs) {
        int options = 6;
        List<Integer> timeLimits = Arrays.asList(0, 2, 5);
        boolean targetStream = false;

        for (String input : inputs) {
            if (input.matches("^\\d+$")) {
                options = Integer.parseInt(input);
                if (options < 2 || options > 10) {
                    PollBot.sendMessage(streamId, "Number of options must be between 2 and 10");
                    return null;
                }
            } else if (input.matches("^\\d+(,\\d+)+$")) {
                timeLimits = Arrays.stream(input.split(","))
                    .map(Integer::parseInt)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                if (timeLimits.size() > 10) {
                    PollBot.sendMessage(streamId, "Number of time limits should be 10 or lower");
                    return null;
                }
            } else if (input.equalsIgnoreCase("room")) {
                targetStream = true;
            }
        }
        return new PollConfig(options, timeLimits, targetStream);
    }

    private boolean userHasActivePoll(User user) {
        if (dataService.hasActivePoll(user.getUserId())) {
            String msg = String.format("<mention uid=\"%d\"/> You already have an existing active poll. " +
                "Use <b>/endpoll</b> to end it before starting a new one", user.getUserId());
            PollBot.sendMessage(PollBot.getImStreamId(user.getUserId()), msg);
            log.info("User {} has an existing active poll. Refusing to create a new one.", user.getDisplayName());
            return true;
        }
        return false;
    }

    private void handleSendCreatePollForm(String streamId, StreamTypes streamType, User user, PollConfig pollConfig) {
        // Reject poll creation if an active one exists
        if (userHasActivePoll(user)) {
            return;
        }
        log.info("Get new poll form via {} requested by {}", streamType, user.getDisplayName());

        boolean showPersonSelector = !pollConfig.isTargetStream() && streamType == StreamTypes.IM;
        String targetStreamId = null;
        if (streamType == StreamTypes.ROOM) {
            targetStreamId = streamId;
        } else if (pollConfig.isTargetStream()) {
            targetStreamId = "";
        }

        String pollML = MarkupService.pollCreateTemplate;
        String data = MarkupService.getPollCreateData(
            showPersonSelector,
            targetStreamId,
            pollConfig.getOptions(),
            pollConfig.getTimeLimits()
        );
        String imStreamId = PollBot.getImStreamId(user.getUserId());
        PollBot.sendMessage(imStreamId, pollML, data);

        log.info("New poll form sent to {} stream {}", streamType, imStreamId);
    }

    @SuppressWarnings("unchecked")
    public void handleCreatePoll(User initiator, SymphonyElementsAction action) {
        // Reject poll creation if an active one exists
        if (userHasActivePoll(initiator)) {
            return;
        }

        Map<String, Object> formValues = action.getFormValues();
        log.info("New poll by {} creation in progress: {}", initiator.getDisplayName(), formValues.toString());

        // Collate options
        Map<String, String> answersMap = new LinkedHashMap<>();
        formValues.entrySet().stream()
            .filter(k -> k.getKey().startsWith("option"))
            .map(entry -> MessageUtils.escapeText(entry.getValue().toString().trim()))
            .filter(answer -> !answer.isEmpty())
            .forEach(answer -> answersMap.putIfAbsent(answer.toLowerCase(), answer));
        List<String> answers = new ArrayList<>(answersMap.values());

        if (answers.size() < 2) {
            String rejectMsg = String.format("<mention uid=\"%d\"/> Your poll contains less than 2 valid options",
                initiator.getUserId());
            PollBot.sendMessage(PollBot.getImStreamId(initiator.getUserId()), rejectMsg);
            log.info("Create poll by {} rejected as there are less than 2 valid options", initiator.getDisplayName());
            return;
        }

        // Validate stream id if provided
        String targetStreamId = action.getStreamId();
        if (formValues.containsKey("targetStreamId")) {
            StreamInfo streamInfo = null;
            String tryTargetStreamId = MessageUtils.escapeStreamId(formValues.get("targetStreamId").toString());

            try {
                tryTargetStreamId = URLEncoder.encode(tryTargetStreamId, StandardCharsets.UTF_8.name());
                log.info("Looking up stream id: {}", tryTargetStreamId);
                streamInfo = PollBot.getBotClient().getStreamsClient().getStreamInfo(tryTargetStreamId);
            } catch (UnsupportedEncodingException e) {
                log.error("Unable to URI encode stream id: {}", tryTargetStreamId);
            } catch (SymClientException e) {
                log.info("Invalid stream id: {}", tryTargetStreamId);
            }

            boolean isMember = false;
            if (streamInfo != null && streamInfo.getStreamType().getType() == StreamTypes.ROOM) {
                try {
                    isMember = PollBot.getBotClient().getStreamsClient().getRoomMembers(tryTargetStreamId) != null;
                    targetStreamId = tryTargetStreamId;
                    log.info("Using stream id for room: {}", streamInfo.getRoomAttributes().getName());
                } catch (ForbiddenException e) {
                    log.error("I am not a member of this room: {}", tryTargetStreamId);
                }
            }

            if (streamInfo == null || streamInfo.getStreamType().getType() != StreamTypes.ROOM || !isMember) {
                String rejectMsg = (streamInfo == null || streamInfo.getStreamType().getType() != StreamTypes.ROOM) ?
                    "Your room stream id is invalid" : "I am not a member in that room";

                PollBot.sendMessage(
                    PollBot.getImStreamId(initiator.getUserId()),
                    String.format("<mention uid=\"%d\"/> %s: <b>%s</b>", initiator.getUserId(), rejectMsg, tryTargetStreamId)
                );

                if (streamInfo != null) {
                    log.info("Stream id is not a room: {}", tryTargetStreamId);
                }
                return;
            }
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
            .creatorDisplayName(initiator.getDisplayName())
            .created(Instant.now())
            .timeLimit(timeLimit)
            .questionText(MessageUtils.escapeText(formValues.get("question").toString()))
            .streamId(targetStreamId)
            .participants(participants)
            .answers(answers)
            .build();
        dataService.createPoll(poll);

        // Construct poll form and blast to audience
        String blastPollML = MarkupService.pollBlastTemplate;
        String blastPollData = MarkupService.getPollBlastData(poll);

        if (participants != null) {
            participants.forEach(
                participant -> PollBot.sendMessage(participant.getImStreamId(), blastPollML, blastPollData)
            );
        } else {
            PollBot.sendMessage(targetStreamId, blastPollML, blastPollData);
        }

        // Start timer
        String endPollByTimerNote = "";
        if (timeLimit > 0) {
            Timer timer = new Timer("PollTimer" + poll.getId());
            timer.schedule(new TimerTask() {
                public void run() {
                    handleEndPoll(null, poll.getCreator(), null);
                }
            }, 60000L * timeLimit);

            endPollByTimerNote = " or wait " + timeLimit + " minute" + (timeLimit > 1 ? "s" : "");
        }

        PollBot.sendMessage(
            PollBot.getImStreamId(initiator.getUserId()),
            String.format(
                "<mention uid=\"%d\"/> Your poll has been created. You can use <b>/endpoll</b>%s to end this poll: <i>%s</i>",
                initiator.getUserId(), endPollByTimerNote, poll.getQuestionText()
            )
        );
        log.info("New poll by {} creation complete", initiator.getDisplayName());
    }

    public void handleSubmitVote(User initiator, SymphonyElementsAction action) {
        String answerIndexString = action.getFormValues().get("action").toString().replace("option-", "");
        int answerIndex = Integer.parseInt(answerIndexString);

        String pollId = action.getFormId().replace("poll-blast-form-", "");
        Poll poll = dataService.getPoll(pollId);

        long initiatorId = initiator.getUserId();
        String initiatorName = initiator.getDisplayName();
        String initiatorStreamId = PollBot.getImStreamId(initiatorId);

        if (poll == null) {
            PollBot.sendMessage(initiatorStreamId, "You have submitted a vote for an invalid poll");
            log.info("Invalid vote cast by {} on stream {}", initiatorName, action.getStreamId());
            return;
        }

        String answer = poll.getAnswers().get(answerIndex);

        if (poll.getEnded() != null) {
            String pollEndedMsg = String.format("This poll has ended and no longer accepts votes: <i>%s</i>",
                poll.getQuestionText());
            PollBot.sendMessage(initiatorStreamId, pollEndedMsg);
            log.info("Rejected vote [{}] cast by {} in stream {} on expired poll: {}",
                answer, initiatorName, action.getStreamId(), poll.getQuestionText());
            return;
        }

        String response, creatorNotification;
        if (dataService.hasVoted(initiatorId, pollId)) {
            dataService.changeVote(initiatorId, pollId, answer);
            response = String.format("Your vote has been updated to <b>%s</b> for the poll: <i>%s</i>",
                answer, poll.getQuestionText());
            creatorNotification = String.format("has changed their vote to: <b>%s</b>", answer);
            log.info("Vote updated to [{}] on poll {} by {}", answer, poll.getId(), initiatorName);
        } else {
            PollVote vote = PollVote.builder().pollId(pollId).answer(answer).userId(initiatorId).build();
            dataService.createVote(vote);
            response = String.format("Thanks for voting <b>%s</b> for the poll: <i>%s</i>", answer, poll.getQuestionText());
            creatorNotification = String.format("has voted for: <b>%s</b>", answer);
            log.info("New vote [{}] cast on poll {} by {}", answer, poll.getId(), initiatorName);
        }

        PollBot.sendMessage(initiatorStreamId, response);
        PollBot.sendMessage(PollBot.getImStreamId(poll.getCreator()), creatorNotification);
    }

    private void handleRigPoll(String streamId, long userId, String displayName) {
        log.info("Rig poll requested by {}", displayName);

        Poll pollToRig = dataService.getActivePoll(userId);
        if (pollToRig == null) {
            PollBot.sendMessage(streamId, "You have no active poll to rig");
            log.info("User {} has no active poll to rig", displayName);
            return;
        }

        List<PollVote> votes = new ArrayList<>();
        List<String> randomAnswers = new ArrayList<>(pollToRig.getAnswers());
        Collections.shuffle(randomAnswers);
        int answersSize = pollToRig.getAnswers().size();
        int rigVolume = ThreadLocalRandom.current().nextInt(17, 77);
        for (int i=0; i < answersSize; i++) {
            for (int r = 0; r < rigVolume; r++) {
                votes.add(PollVote.builder()
                    .pollId(pollToRig.getId())
                    .answer(randomAnswers.get(i))
                    .build());
            }
            rigVolume += (Math.random() * 387) - (Math.random() * 27);
        }
        dataService.createVotes(votes);

        PollBot.sendMessage(streamId, "Your active poll has been rigged");
        log.info("User {} has rigged active poll", displayName);
    }

    private void handleEndPoll(String streamId, long userId, String displayName) {
        log.info("End poll requested by {}", displayName != null ? displayName : "[Timer]");

        Poll poll = dataService.getActivePoll(userId);
        if (poll == null) {
            if (streamId != null) {
                PollBot.sendMessage(streamId, "You have no active poll to end");
                log.info("User {} has no active poll to end", displayName);
            } else {
                log.info("Poll by {} time limit reached but poll was already ended", userId);
            }
            return;
        }

        List<PollVote> votes = dataService.getVotes(poll.getId());
        String response, data = null;

        if (votes.isEmpty()) {
            response = String.format("<mention uid=\"%d\" /> Poll ended but with no results to show", poll.getCreator());
            log.info("Poll {} ended with no votes", poll.getId());
        } else {
            // Aggregate vote results
            List<PollResult> pollResults = new ArrayList<>(dataService.getPollResults(poll.getId()));

            // Add in widths
            long maxVal = Collections.max(pollResults, Comparator.comparingLong(PollResult::getCount)).getCount();
            pollResults.forEach(r -> r.setWidth(Math.max(1, (int) (((float) r.getCount() / maxVal) * 200))));

            // Add in 0 votes for options nobody voted on
            poll.getAnswers().stream()
                .map(PollResult::new)
                .filter(a -> !pollResults.contains(a))
                .forEach(pollResults::add);

            response = MarkupService.pollResultsTemplate;
            data = MarkupService.convertToJsonString(PollResultsData.builder()
                .creatorDisplayName(poll.getCreatorDisplayName())
                .question(poll.getQuestionText())
                .results(pollResults)
                .build());

            log.info("Poll {} ended with results {}", poll.getId(), pollResults.toString());
        }

        dataService.endPoll(poll.getCreator());
        PollBot.sendMessage(poll.getStreamId(), response, data);
    }

    private void handleHistory(String streamId, StreamTypes streamType, long userId, String displayName) {
        log.info("Poll history requested by {}", displayName);

        String targetStreamId = streamType == StreamTypes.ROOM ? streamId : null;
        PollHistory pollHistory = dataService.getPollHistory(userId, targetStreamId, displayName);

        if (pollHistory == null) {
            String roomSuffix = streamType == StreamTypes.ROOM ? " for this room" : "";
            PollBot.sendMessage(streamId, String.format("<mention uid=\"%d\"/> You have no poll history%s.", userId, roomSuffix));
            return;
        }

        String pollHistoryTemplate = MarkupService.pollHistoryTemplate;

        PollBot.sendMessage(streamId, pollHistoryTemplate, MarkupService.convertToJsonString(pollHistory));
    }
}
