package com.symphony.ps.pollbot.cmd;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.spring.annotation.Slash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.Collections.singletonMap;

@Component
@RequiredArgsConstructor
public class HelpCmd {

    private final MessageService messageService;

    @Slash(value = "/help", mentionBot = false)
    public void help(CommandContext context) {

        final boolean isIM = context.getSourceEvent().getMessage().getStream().getStreamType().equals("IM");

        this.messageService.send(
                context.getStreamId(),
                this.messageService.templates().newTemplateFromClasspath("/help.ftl")
                        .process(singletonMap("isIM", isIM))
        );
    }
}
