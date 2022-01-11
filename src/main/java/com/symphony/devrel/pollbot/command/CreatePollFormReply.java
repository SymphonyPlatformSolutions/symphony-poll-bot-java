package com.symphony.devrel.pollbot.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.symphony.bdk.core.activity.ActivityMatcher;
import com.symphony.bdk.core.activity.form.FormReplyActivity;
import com.symphony.bdk.core.activity.form.FormReplyContext;
import com.symphony.bdk.core.activity.model.ActivityInfo;
import com.symphony.bdk.core.activity.model.ActivityType;
import com.symphony.bdk.core.service.datafeed.EventException;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.message.util.MessageUtil;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageBlastResponse;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.devrel.pollbot.model.Poll;
import com.symphony.devrel.pollbot.model.PollBlastData;
import com.symphony.devrel.pollbot.model.PollParticipant;
import com.symphony.devrel.pollbot.service.DataService;
import com.symphony.devrel.pollbot.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreatePollFormReply extends FormReplyActivity<FormReplyContext> {
    private final PollService pollService;
    private final DataService dataService;
    private final MessageService messageService;
    private final StreamService streamService;
    private final EndPollCommand endPollCommand;

    @Override
    protected ActivityMatcher<FormReplyContext> matcher() throws EventException {
        return context -> context.getFormId().equals("poll-create-form");
    }

    @Override
    protected void onActivity(FormReplyContext context) throws EventException {
        // Reject poll creation if an active one exists
        V4User initiator = context.getInitiator().getUser();
        if (pollService.userHasActivePoll(initiator.getUserId())) {
            return;
        }

        log.info("New poll by {} creation in progress: {}", initiator.getDisplayName(), context.getFormValues().toPrettyString());

        // Collate options
        Map<String, String> answersMap = new LinkedHashMap<>();
        context.getFormValues().fields().forEachRemaining(entry -> {
            if (entry.getKey().startsWith("option")) {
                String value = MessageUtil.escapeSpecialChars(entry.getValue().asText().trim());
                if (!value.isEmpty()) {
                    answersMap.putIfAbsent(value.toLowerCase(), value);
                }
            }
        });
        List<String> answers = new ArrayList<>(answersMap.values());

        if (answers.size() < 2) {
            String rejectMsg = String.format("<mention uid=\"%d\"/> Your poll contains less than 2 valid options",
                initiator.getUserId());
            messageService.send(dataService.getImStreamId(initiator.getUserId()), rejectMsg);
            log.info("Create poll by {} rejected as there are less than 2 valid options", initiator.getDisplayName());
            return;
        }

        // Validate stream id if provided
        String targetStreamId = context.getSourceEvent().getStream().getStreamId();
        if (context.getFormValue("targetStreamId") != null) {
            String testStreamId = escapeStreamId(context.getFormValue("targetStreamId"));
            String rejectMsg = null;

            try {
                log.info("Looking up stream id: {}", testStreamId);
                V3RoomDetail roomInfo = streamService.getRoomInfo(testStreamId);

                if (streamService.listRoomMembers(testStreamId) == null) {
                    rejectMsg = "I am not a member in that room";
                } else if (roomInfo.getRoomAttributes().getReadOnly()) {
                    rejectMsg = "Room is read-only";
                }
            } catch (Exception e) {
                log.info("Invalid stream id: {}", testStreamId);
                rejectMsg = "Your room stream id is invalid";
            }

            if (rejectMsg != null) {
                messageService.send(
                    context.getSourceEvent().getStream().getStreamId(),
                    String.format("<mention uid=\"%d\"/> %s: <b>%s</b>", initiator.getUserId(), rejectMsg, testStreamId)
                );
                return;
            }
            targetStreamId = testStreamId;
        }

        // Obtain IM stream IDs if audience is specified
        List<PollParticipant> participants = new ArrayList<>();
        if (context.getFormValue("audience") != null) {
            ArrayNode audience = (ArrayNode) context.getFormValues().get("audience");
            audience.elements().forEachRemaining(person -> participants.add(
                PollParticipant.builder()
                    .userId(person.asLong())
                    .imStreamId(dataService.getImStreamId(person.asLong()))
                    .build()
            ));
        }

        int timeLimit = 0;
        try {
            String timeLimitString = context.getFormValue("timeLimit");
            if (!timeLimitString.isEmpty()) {
                timeLimit = Integer.parseInt(timeLimitString);
            }
        } catch (NumberFormatException e) {
            messageService.send(context.getSourceEvent().getStream(), "Create poll rejected: Expiry should be a number");
            return;
        }
        if (timeLimit < 0 || timeLimit > 1440) {
            messageService.send(context.getSourceEvent().getStream(), "Create poll rejected: Expiry should be between 0 and 1440");
            return;
        }

        // Create poll object
        Poll poll = Poll.builder()
            .creator(initiator.getUserId())
            .creatorDisplayName(initiator.getDisplayName())
            .created(Instant.now())
            .timeLimit(timeLimit)
            .questionText(MessageUtil.escapeSpecialChars(context.getFormValue("question")))
            .streamId(targetStreamId)
            .participants(participants)
            .answers(answers)
            .build();
        dataService.savePoll(poll);

        // Construct poll form and blast to audience
        Map<String, ?> data = Map.of("data", PollBlastData.builder()
            .id(poll.getId())
            .question(poll.getQuestionText())
            .answers(poll.getAnswers())
            .creatorDisplayName(poll.getCreatorDisplayName())
            .timeLimit(poll.getTimeLimit())
            .build());
        Message message = Message.builder().content(pollService.getMessage("poll-blast-form", data)).build();

        List<String> messageIds;

        if (!participants.isEmpty()) {
            List<String> streamIds = participants.stream().map(PollParticipant::getImStreamId).toList();
            V4MessageBlastResponse response = messageService.send(streamIds, message);
            messageIds = response.getMessages().stream().map(V4Message::getMessageId).toList();
        } else {
            V4Message response = messageService.send(targetStreamId, message);
            messageIds = List.of(response.getMessageId());
        }
        poll.setMessageIds(messageIds);

        // Start timer
        String endPollByTimerNote = "";
        if (timeLimit > 0) {
            Timer timer = new Timer("PollTimer" + poll.getId());
            timer.schedule(new TimerTask() {
                public void run() {
                    endPollCommand.endPoll(poll.getCreator());
                }
            }, 60000L * timeLimit);

            endPollByTimerNote = "This poll will automatically end in " + timeLimit + " minute" + (timeLimit > 1 ? "s" : "");
        }

        Map<String, ?> pollCreatedData = Map.of("data", Map.of(
            "message", "Your poll has been created",
            "question", poll.getQuestionText(),
            "endPollByTimerNote", endPollByTimerNote
        ));
        String endPollMessage = pollService.getMessage("poll-created", pollCreatedData);
        V4Message statusMessage = messageService.send(dataService.getImStreamId(initiator.getUserId()), endPollMessage);
        log.info("New poll by {} creation complete", initiator.getDisplayName());

        // Persist poll object to database
        poll.setStatusMessageId(statusMessage.getMessageId());
        dataService.savePoll(poll);
    }

    @Override
    protected ActivityInfo info() {
        return new ActivityInfo()
            .name("Respond to poll creation forms")
            .type(ActivityType.FORM);
    }

    private String escapeStreamId(String rawStreamId) {
        return rawStreamId.trim()
            .replaceAll("[=]+$", "")
            .replaceAll("\\+", "-")
            .replaceAll("/", "_");
    }
}
