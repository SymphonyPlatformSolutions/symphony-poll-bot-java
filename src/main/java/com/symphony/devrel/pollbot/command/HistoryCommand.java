package com.symphony.devrel.pollbot.command;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.StreamType.TypeEnum;
import com.symphony.bdk.spring.annotation.Slash;
import com.symphony.devrel.pollbot.model.MessageMetadata;
import com.symphony.devrel.pollbot.model.PollData;
import com.symphony.devrel.pollbot.model.PollHistory;
import com.symphony.devrel.pollbot.service.DataService;
import com.symphony.devrel.pollbot.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryCommand {
    private final PollService pollService;
    private final DataService dataService;
    private final MessageService messageService;

    @Slash("/history")
    public void getHistory(CommandContext context) {
        handleHistory(context, 10, false);
    }

    @Slash("/history {countArg}")
    public void getHistoryCustom(CommandContext context, String countArg) {
        try {
            int count = Integer.parseInt(countArg);
            handleHistory(context, count, false);
        } catch (NumberFormatException e) {
            messageService.send(context.getStreamId(), "Usage: /history <count>");
        }
    }

    @Slash("/active")
    public void getActive(CommandContext context) {
        handleHistory(context, 1, true);
    }

    private void handleHistory(CommandContext context, int count, boolean isActive) {
        MessageMetadata meta = pollService.getMeta(context);

        String noPollMsg = "You have no poll history%s.";
        if (isActive) {
            log.info("Active poll requested by {}", meta.getDisplayName());
            noPollMsg = "You have no active poll%s.";
        } else {
            log.info("History of past {} polls requested by {}", count, meta.getDisplayName());
        }

        String targetStreamId = meta.getStreamType() == TypeEnum.ROOM ? meta.getStreamId() : null;
        PollHistory pollHistory = dataService.getPollHistory(meta.getUserId(), targetStreamId, meta.getDisplayName(), count, isActive);

        if (pollHistory == null) {
            String roomSuffix = meta.getStreamType() == TypeEnum.ROOM ? " for this room" : "";
            messageService.send(meta.getStreamId(), String.format(noPollMsg, roomSuffix));
            return;
        }

        Map<String, PollData> data = Map.of("data", pollHistory);
        String message = pollService.getMessage("poll-history", data);
        messageService.send(meta.getStreamId(), message);
    }
}
