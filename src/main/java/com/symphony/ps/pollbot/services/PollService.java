package com.symphony.ps.pollbot.services;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.*;
import com.symphony.bdk.template.api.Template;
import com.symphony.ps.pollbot.model.*;
import com.symphony.ps.pollbot.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PollService {
    private final DataService dataService;
    private final StreamService streamService;
    private final MessageService messageService;
    private static final Map<Long, String> userImMap = new HashMap<>();

    public PollService(DataService dataService, StreamService streamService, MessageService messageService) {
        this.dataService = dataService;
        this.streamService = streamService;
        this.messageService = messageService;
    }

    private static final String helpML = "<ul>" +
        "<li><b>/poll</b>: Get a standard create poll form</li>" +
        "<li><b>/poll room</b>: Get a create poll form that targets a room via stream id</li>" +
        "<li><b>/poll 4 0,5,15</b>: Get a create poll form with 4 options and time limits of None, 5 and 15 minutes</li>" +
        "<li><b>/endpoll</b>: End your active poll</li>" +
        "<li><b>/history</b>: View your personal poll history</li>" +
        "<li><b>/active</b>: Preview the results of your active poll</li>" +
        "</ul>";

    public void handleIncomingMessage(V4Message msg, StreamType.TypeEnum streamType) throws PresentationMLParserException {
        long userId = msg.getUser().getUserId();
        String displayName = msg.getUser().getDisplayName();
        String streamId = msg.getStream().getStreamId();
        String msgText = PresentationMLParser.getTextContent(msg.getMessage());
        if (msgText == null) {
            log.info("Ignoring message as message text cannot be parsed");
            return;
        }
        String[] msgParts = msgText.trim().toLowerCase().split(" ", 2);

        switch (msgParts[0]) {
            case "/help":
                String helpMLForStream = streamType == StreamType.TypeEnum.IM ? helpML
                    : helpML.replace("</li></ul>", " for this room</li></ul>");
                messageService.send(streamId, helpMLForStream);
                break;

            case "/poll":
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
                int count = 10;
                if (msgParts.length > 1) {
                    try {
                        count = Integer.parseInt(msgParts[1]);
                        if (count > 30) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        messageService.send(streamId, "Invalid history count provided, using 10 instead");
                    }
                }
                handleHistory(streamId, streamType, userId, displayName, count, false);
                break;

            case "/active":
                handleHistory(streamId, streamType, userId, displayName, 1, true);
                break;

            default:
        }
    }

    private PollConfig parseConfigInput(String streamId, String[] inputs) {
        int options = 6;
        List<Integer> timeLimits = Arrays.asList(0, 2, 5);
        boolean targetStream = false;

        for (String input : inputs) {
            if (input.matches("^\\d+$")) {
                options = Integer.parseInt(input);
                if (options < 2 || options > 10) {
                    messageService.send(streamId, "Number of options must be between 2 and 10");
                    return null;
                }
            } else if (input.matches("^\\d+(,\\d+)+$")) {
                timeLimits = Arrays.stream(input.split(","))
                    .map(Integer::parseInt)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                if (timeLimits.size() > 10) {
                    messageService.send(streamId, "Number of time limits should be 10 or lower");
                    return null;
                }
            } else if (input.equalsIgnoreCase("room")) {
                targetStream = true;
            }
        }
        return new PollConfig(options, timeLimits, targetStream);
    }

    private boolean userHasActivePoll(V4User user) {
        if (dataService.hasActivePoll(user.getUserId())) {
            String msg = String.format("<mention uid=\"%d\"/> You already have an existing active poll. " +
                "Use <b>/endpoll</b> to end it before starting a new one", user.getUserId());
            messageService.send(getImStreamId(user.getUserId()), msg);
            log.info("User {} has an existing active poll. Refusing to create a new one.", user.getDisplayName());
            return true;
        }
        return false;
    }

    private void handleSendCreatePollForm(String streamId, StreamType.TypeEnum streamType, V4User user, PollConfig pollConfig) {
        // Reject poll creation if an active one exists
        if (userHasActivePoll(user)) {
            return;
        }
        log.info("Get new poll form via {} requested by {}", streamType, user.getDisplayName());

        boolean showPersonSelector = !pollConfig.isTargetStream() && streamType == StreamType.TypeEnum.IM;
        String targetStreamId = null;
        if (streamType == StreamType.TypeEnum.ROOM) {
            targetStreamId = streamId;
            V3RoomDetail roomInfo = streamService.getRoomInfo(targetStreamId);
            if (roomInfo.getRoomAttributes().getReadOnly()) {
                String imStreamId = getImStreamId(user.getUserId());
                messageService.send(imStreamId, "Unable to create poll in a read-only room");
                return;
            }
        } else if (pollConfig.isTargetStream()) {
            targetStreamId = "";
        }

        String pollML = MarkupService.pollCreateTemplate;
        Map<String, PollData> data = MarkupService.getPollCreateData(
            showPersonSelector,
            targetStreamId,
            pollConfig.getOptions(),
            pollConfig.getTimeLimits()
        );
        String imStreamId = getImStreamId(user.getUserId());
        Template template = messageService.templates().newTemplateFromClasspath(pollML);
        messageService.send(imStreamId, Message.builder().template(template, data).build());

        log.info("New poll form sent to {} stream {}", streamType, imStreamId);
    }

    @SuppressWarnings("unchecked")
    public void handleCreatePoll(V4User initiator, V4SymphonyElementsAction action) {
        // Reject poll creation if an active one exists
        if (userHasActivePoll(initiator)) {
            return;
        }

        Map<String, Object> formValues = (Map<String, Object>) action.getFormValues();
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
            messageService.send(getImStreamId(initiator.getUserId()), rejectMsg);
            log.info("Create poll by {} rejected as there are less than 2 valid options", initiator.getDisplayName());
            return;
        }

        // Validate stream id if provided
        String targetStreamId = action.getStream().getStreamId();
        if (formValues.containsKey("targetStreamId")) {
            String tryTargetStreamId = MessageUtils.escapeStreamId(formValues.get("targetStreamId").toString());
            String rejectMsg = null;

            try {
                log.info("Looking up stream id: {}", tryTargetStreamId);
                V3RoomDetail roomInfo = streamService.getRoomInfo(tryTargetStreamId);

                if (streamService.listRoomMembers(tryTargetStreamId) == null) {
                    rejectMsg = "I am not a member in that room";
                } else if (roomInfo.getRoomAttributes().getReadOnly()) {
                    rejectMsg = "Room is read-only";
                }
            } catch (Exception e) {
                log.info("Invalid stream id: {}", tryTargetStreamId);
                rejectMsg = "Your room stream id is invalid";
            }

            if (rejectMsg != null) {
                messageService.send(
                    getImStreamId(initiator.getUserId()),
                    String.format("<mention uid=\"%d\"/> %s: <b>%s</b>", initiator.getUserId(), rejectMsg, tryTargetStreamId)
                );
                return;
            }
            targetStreamId = tryTargetStreamId;
        }

        // Obtain IM stream IDs if audience is specified
        List<PollParticipant> participants = null;
        if (formValues.get("audience") != null) {
            participants = ((List<Long>) formValues.get("audience"))
                .parallelStream()
                .map(userId -> {
                    String streamId = streamService.create(Collections.singletonList(userId)).getId();
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
        Map<String, PollData> blastPollData = MarkupService.getPollBlastData(poll);

        if (participants != null) {
            participants.forEach(
                participant -> {
                    Template template = messageService.templates().newTemplateFromClasspath(blastPollML);
                    messageService.send(participant.getImStreamId(), Message.builder().template(template, blastPollData).build());
                }
            );
        } else {
            Template template = messageService.templates().newTemplateFromClasspath(blastPollML);
            messageService.send(targetStreamId, Message.builder().template(template, blastPollData).build());
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

        messageService.send(
            getImStreamId(initiator.getUserId()),
            String.format(
                "Your poll has been created. You can use <b>/endpoll</b>%s to end this poll: <i>%s</i>",
                endPollByTimerNote, poll.getQuestionText()
            )
        );
        log.info("New poll by {} creation complete", initiator.getDisplayName());
    }

    @SuppressWarnings("unchecked")
    public void handleSubmitVote(V4User initiator, V4SymphonyElementsAction action) {
        String answerIndexString = ((Map<String, String>) action.getFormValues()).get("action").replace("option-", "");
        int answerIndex = Integer.parseInt(answerIndexString);

        String pollId = action.getFormId().replace("poll-blast-form-", "");
        Poll poll = dataService.getPoll(pollId);

        long initiatorId = initiator.getUserId();
        String initiatorName = initiator.getDisplayName();
        String initiatorStreamId = getImStreamId(initiatorId);

        if (poll == null) {
            messageService.send(initiatorStreamId, "You have submitted a vote for an invalid poll");
            log.info("Invalid vote cast by {} on stream {}", initiatorName, action.getStream().getStreamId());
            return;
        }

        String answer = poll.getAnswers().get(answerIndex);

        if (poll.getEnded() != null) {
            String pollEndedMsg = String.format("This poll has ended and no longer accepts votes: <i>%s</i>",
                poll.getQuestionText());
            messageService.send(initiatorStreamId, pollEndedMsg);
            log.info("Rejected vote [{}] cast by {} in stream {} on expired poll: {}",
                answer, initiatorName, action.getStream().getStreamId(), poll.getQuestionText());
            return;
        }

        String response, creatorNotification;
        if (dataService.hasVoted(initiatorId, pollId)) {
            dataService.changeVote(initiatorId, pollId, answer);
            response = String.format("Your vote has been updated to <b>%s</b> for the poll: <i>%s</i>",
                answer, poll.getQuestionText());
            creatorNotification = String.format("<mention uid=\"%d\"/> has changed their vote to: <b>%s</b>",
                initiatorId, answer);
            log.info("Vote updated to [{}] on poll {} by {}", answer, poll.getId(), initiatorName);
        } else {
            PollVote vote = PollVote.builder().pollId(pollId).answer(answer).userId(initiatorId).build();
            dataService.createVote(vote);
            response = String.format("Thanks for voting <b>%s</b> for the poll: <i>%s</i>", answer, poll.getQuestionText());
            creatorNotification = String.format("<mention uid=\"%d\"/> has voted <b>%s</b> for the poll: <i>%s</i> ",
                initiatorId, answer, poll.getQuestionText());
            log.info("New vote [{}] cast on poll {} by {}", answer, poll.getId(), initiatorName);
        }

        messageService.send(initiatorStreamId, response);
        messageService.send(getImStreamId(poll.getCreator()), creatorNotification);
    }

    private void handleRigPoll(String streamId, long userId, String displayName) {
        log.info("Rig poll requested by {}", displayName);

        Poll pollToRig = dataService.getActivePoll(userId);
        if (pollToRig == null) {
            messageService.send(streamId, "You have no active poll to rig");
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

        messageService.send(streamId, "Your active poll has been rigged");
        log.info("User {} has rigged active poll", displayName);
    }

    private void handleEndPoll(String streamId, long userId, String displayName) {
        log.info("End poll requested by {}", displayName != null ? displayName : "[Timer]");

        Poll poll = dataService.getActivePoll(userId);
        if (poll == null) {
            if (streamId != null) {
                streamId = getImStreamId(userId);
                messageService.send(streamId, "You have no active poll to end");
                log.info("User {} has no active poll to end", displayName);
            } else {
                log.info("Poll by {} time limit reached but poll was already ended", userId);
            }
            return;
        }

        List<PollVote> votes = dataService.getVotes(poll.getId());
        String response;
        Map<String, PollData> data = null;

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
            data = Collections.singletonMap("data", PollResultsData.builder()
                    .creatorDisplayName(poll.getCreatorDisplayName())
                    .question(poll.getQuestionText())
                    .results(pollResults)
                    .build());

            log.info("Poll {} ended with results {}", poll.getId(), pollResults.toString());
        }

        dataService.endPoll(poll.getCreator());
        Template template = messageService.templates().newTemplateFromClasspath(response);
        if (data != null) {
            messageService.send(poll.getStreamId(), Message.builder().template(template, data).build());
        } else {
            messageService.send(poll.getStreamId(), Message.builder().template(template).build());
        }
    }

    private void handleHistory(String streamId, StreamType.TypeEnum streamType, long userId, String displayName, int count,
                               boolean isActive) {
        String noPollMsg = "You have no poll history%s.";
        if (isActive) {
            log.info("Active poll requested by {}", displayName);
            noPollMsg = "You have no active poll%s.";
        } else {
            log.info("History of past {} polls requested by {}", count, displayName);
        }

        String targetStreamId = streamType == StreamType.TypeEnum.ROOM ? streamId : null;
        PollHistory pollHistory = dataService.getPollHistory(userId, targetStreamId, displayName, count, isActive);

        if (pollHistory == null) {
            String roomSuffix = streamType == StreamType.TypeEnum.ROOM ? " for this room" : "";
            messageService.send(streamId, String.format(noPollMsg, roomSuffix));
            return;
        }

        Template template = messageService.templates().newTemplateFromClasspath(MarkupService.pollHistoryTemplate);
        Map<String, PollData> data = Collections.singletonMap("data", pollHistory);
        messageService.send(streamId, Message.builder().template(template, data).build());
    }

    private String getImStreamId(long userId) {
        if (userImMap.containsKey(userId)) {
            return userImMap.get(userId);
        }
        String streamId = streamService.create(userId).getId();
        userImMap.putIfAbsent(userId, streamId);
        return streamId;
    }
}
