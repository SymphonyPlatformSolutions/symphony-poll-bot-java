package com.symphony.devrel.pollbot.command;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.gen.api.model.StreamType;
import com.symphony.bdk.gen.api.model.StreamType.TypeEnum;
import com.symphony.bdk.spring.annotation.Slash;
import com.symphony.devrel.pollbot.model.MessageMetadata;
import com.symphony.devrel.pollbot.model.PollCreateData;
import com.symphony.devrel.pollbot.model.PollData;
import com.symphony.devrel.pollbot.service.DataService;
import com.symphony.devrel.pollbot.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreatePollCommand {
    private final DataService dataService;
    private final PollService pollService;
    private final MessageService messageService;
    private final SessionService session;
    private static final int DEFAULT_NUMBER_OF_OPTIONS = 6;

    @Slash(value="/poll", mentionBot=false)
    public void getPollFormDeprecated(CommandContext context) {
        long myId = session.getSession().getId();
        String imStreamId = dataService.getImStreamId(context.getInitiator().getUser().getUserId());
        messageService.send(imStreamId, "@mention prefix is now required:<br/><mention uid=\"" + myId + "\" /> /poll");
    }

    @Slash("/poll")
    public void getPollForm(CommandContext context) {
        sendCreatePollForm(context, DEFAULT_NUMBER_OF_OPTIONS, null);
    }

    @Slash("/poll {arg}")
    public void getPollFormCustom(CommandContext context, String arg) {
        try {
            int options = Integer.parseInt(arg);
            if (options < 2 || options > 10) {
                messageService.send(context.getStreamId(), "Options should be between 2 and 10");
                return;
            }
            sendCreatePollForm(context, options, null);
        } catch (NumberFormatException e) {
            sendCreatePollForm(context, DEFAULT_NUMBER_OF_OPTIONS, arg);
        }
    }

    private void sendCreatePollForm(CommandContext context, int numberOfOptions, String targetStreamId) {
        MessageMetadata meta = pollService.getMeta(context);

        // Reject poll creation if an active one exists
        if (pollService.userHasActivePoll(meta.getUserId())) {
            return;
        }
        log.info("Get new poll form via {} requested by {}", meta.getStreamType(), meta.getDisplayName());

        String streamId = context.getStreamId();
        TypeEnum streamType = meta.getStreamType();

        if (targetStreamId != null) {
            try {
                streamType = dataService.getStreamType(targetStreamId);
                streamId = targetStreamId;
            } catch (Exception e) {
                messageService.send(streamId, "Invalid stream id");
                return;
            }
        }

        String imStreamId = dataService.getImStreamId(meta.getUserId());
        if (streamType == StreamType.TypeEnum.ROOM) {
            if (dataService.isRoomReadOnly(streamId)) {
                messageService.send(imStreamId, "Unable to create poll in a read-only room");
                return;
            }
            targetStreamId = streamId;
        }

        Map<String, PollData> data = Map.of("data",
            PollCreateData.builder()
                .showPersonSelector(streamType == TypeEnum.IM)
                .count(numberOfOptions)
                .targetStreamId(targetStreamId)
                .build()
        );

        String message = pollService.getMessage("poll-create-form", data);
        messageService.send(imStreamId, message);

        log.info("New poll form for {} sent to {}", streamType, imStreamId);
    }
}
