package com.symphony.devrel.pollbot.service;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.StreamType.TypeEnum;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.devrel.pollbot.model.MessageMetadata;
import com.symphony.devrel.pollbot.model.Poll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {
    private final DataService dataService;
    private final MessageService messageService;

    public MessageMetadata getMeta(CommandContext context) {
        V4User user = context.getInitiator().getUser();
        TypeEnum streamType = TypeEnum.fromValue(
            context.getSourceEvent().getMessage().getStream().getStreamType());
        return MessageMetadata.builder()
            .streamType(streamType)
            .streamId(context.getStreamId())
            .userId(user.getUserId())
            .displayName(user.getDisplayName())
            .build();
    }

    public boolean userHasActivePoll(long userId) {
        if (dataService.hasActivePoll(userId)) {
            Poll activePoll = dataService.getActivePoll(userId);
            String endPollByTimerNote = "Click the button below to end the poll now";
            endPollByTimerNote += (activePoll.getTimeLimit() > 0) ? " or wait for the time limit to expire" : "";
            Map<String, ?> pollRejectedData = Map.of("data", Map.of(
                "message", "You already have an existing active poll",
                "question", activePoll.getQuestionText(),
                "endPollByTimerNote", endPollByTimerNote
            ));
            String msg = getMessage("poll-created", pollRejectedData);
            messageService.send(dataService.getImStreamId(userId), msg);
            log.info("User {} has an existing active poll. Refusing to create a new one.", userId);
            return true;
        }
        return false;
    }

    public String getMessage(String template, Map<String, ?> data) {
        return messageService.templates()
            .newTemplateFromClasspath("/templates/" + template + ".ftl")
            .process(data);
    }
}
