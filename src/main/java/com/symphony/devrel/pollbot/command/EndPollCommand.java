package com.symphony.devrel.pollbot.command;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.health.HealthService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.spring.annotation.Slash;
import com.symphony.devrel.pollbot.model.*;
import com.symphony.devrel.pollbot.service.DataService;
import com.symphony.devrel.pollbot.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndPollCommand {
    private final PollService pollService;
    private final DataService dataService;
    private final MessageService messageService;
    private final HealthService healthService;
    private final SessionService session;
    private boolean msgUpdateSupported;

    @PostConstruct
    public void init() {
        String[] versionParts = healthService.getAgentInfo().getVersion().substring(6).split("\\.");
        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);
        msgUpdateSupported = major > 20 || (major == 20 && minor >= 13);
        if (!msgUpdateSupported) {
            log.warn("Message Update not supported yet on Agent {}.{}", major, minor);
        }
    }

    @Slash(value="/endpoll", mentionBot=false)
    public void endPollDeprecated(CommandContext context) {
        long myId = session.getSession().getId();
        String imStreamId = dataService.getImStreamId(context.getInitiator().getUser().getUserId());
        messageService.send(imStreamId, "@mention prefix is now required:<br/><mention uid=\"" + myId + "\" /> /endpoll");
    }

    @Slash("/endpoll")
    public void endPoll(CommandContext context) {
        MessageMetadata meta = pollService.getMeta(context);
        endPoll(meta);
    }

    public void endPoll(long userId) {
        endPoll(MessageMetadata.builder().userId(userId).build());
    }

    private void endPoll(MessageMetadata meta) {
        String displayName = meta.getDisplayName();
        log.info("End poll requested by {}", displayName != null ? displayName : "[Timer]");

        Poll poll = dataService.getActivePoll(meta.getUserId());
        if (poll == null) {
            if (meta.getStreamId() != null) {
                String streamId = dataService.getImStreamId(meta.getUserId());
                messageService.send(streamId, "You have no active poll to end");
                log.info("User {} has no active poll to end", displayName);
            } else {
                log.info("Poll by {} time limit reached but poll was already ended", meta.getUserId());
            }
            return;
        }

        List<PollVote> votes = dataService.getVotes(poll.getId());
        String template;
        Map<String, ?> data;

        if (votes.isEmpty()) {
            template = "poll-results-empty";
            data = Map.of("uid", poll.getCreator());
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

            template = "poll-results";
            data = Map.of("data", PollResultsData.builder()
                .creatorDisplayName(poll.getCreatorDisplayName())
                .question(poll.getQuestionText())
                .results(pollResults)
                .build());

            log.info("Poll {} ended with results {}", poll.getId(), pollResults);
        }

        if (poll.getMessageIds() != null) {
            poll.getMessageIds().forEach(messageId -> {
                if (msgUpdateSupported) {
                    String streamId = messageService.getMessage(messageId).getStream().getStreamId();
                    try {
                        messageService.update(streamId, messageId, Message.builder().content("This poll has ended").build());
                    } catch (Exception e) {
                        msgUpdateSupported = false;
                        messageService.suppressMessage(messageId);
                    }
                } else {
                    messageService.suppressMessage(messageId);
                }
            });
        }

        dataService.endPoll(poll.getCreator());
        String message = pollService.getMessage(template, data);
        messageService.send(poll.getStreamId(), message);
    }
}
