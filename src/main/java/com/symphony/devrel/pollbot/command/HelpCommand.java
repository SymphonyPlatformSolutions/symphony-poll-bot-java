package com.symphony.devrel.pollbot.command;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.spring.annotation.Slash;
import com.symphony.devrel.pollbot.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HelpCommand {
    private final PollService pollService;
    private final MessageService messageService;
    private final SessionService sessionService;

    @Slash("/help")
    public void help(CommandContext context) {
        boolean isIM = context.getSourceEvent().getMessage().getStream().getStreamType().equals("IM");
        String message = pollService.getMessage("help", Map.of(
            "isIM", isIM,
            "userId", sessionService.getSession().getId()
        ));
        messageService.send(context.getStreamId(), message);
    }
}
