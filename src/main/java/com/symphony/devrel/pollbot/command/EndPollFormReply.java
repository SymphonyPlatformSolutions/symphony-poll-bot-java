package com.symphony.devrel.pollbot.command;

import com.symphony.bdk.core.activity.ActivityMatcher;
import com.symphony.bdk.core.activity.form.FormReplyActivity;
import com.symphony.bdk.core.activity.form.FormReplyContext;
import com.symphony.bdk.core.activity.model.ActivityInfo;
import com.symphony.bdk.core.activity.model.ActivityType;
import com.symphony.bdk.core.service.datafeed.EventException;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.devrel.pollbot.model.MessageMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndPollFormReply extends FormReplyActivity<FormReplyContext> {
    private final EndPollCommand endPollCommand;

    @Override
    protected ActivityMatcher<FormReplyContext> matcher() throws EventException {
        return context -> context.getFormId().equals("poll-end-form");
    }

    @Override
    protected void onActivity(FormReplyContext context) throws EventException {
        V4User user = context.getInitiator().getUser();
        MessageMetadata meta = MessageMetadata.builder()
            .streamId(context.getSourceEvent().getStream().getStreamId())
            .userId(user.getUserId())
            .displayName(user.getDisplayName())
            .build();
        endPollCommand.endPoll(meta);
    }

    @Override
    protected ActivityInfo info() {
        return new ActivityInfo().name("Respond to end poll forms").type(ActivityType.FORM);
    }
}
