package com.symphony.devrel.pollbot.command;

import com.symphony.bdk.core.activity.ActivityMatcher;
import com.symphony.bdk.core.activity.form.FormReplyActivity;
import com.symphony.bdk.core.activity.form.FormReplyContext;
import com.symphony.bdk.core.activity.model.ActivityInfo;
import com.symphony.bdk.core.activity.model.ActivityType;
import com.symphony.bdk.core.service.datafeed.EventException;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.devrel.pollbot.model.Poll;
import com.symphony.devrel.pollbot.model.PollVote;
import com.symphony.devrel.pollbot.service.DataService;
import com.symphony.devrel.pollbot.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitVoteFormReply extends FormReplyActivity<FormReplyContext> {
    private final PollService pollService;
    private final MessageService messageService;
    private final DataService dataService;

    @Override
    protected ActivityMatcher<FormReplyContext> matcher() throws EventException {
        return context -> context.getFormId().matches("poll-blast-form-[\\w\\d]+");
    }

    @Override
    protected void onActivity(FormReplyContext context) throws EventException {
        int answerIndex = Integer.parseInt(context.getFormValue("action").substring(7));

        String pollId = context.getFormId().replace("poll-blast-form-", "");
        Poll poll = dataService.getPoll(pollId);

        long initiatorId = context.getInitiator().getUser().getUserId();
        String initiatorName = context.getInitiator().getUser().getDisplayName();
        String initiatorStreamId = dataService.getImStreamId(initiatorId);
        String streamId = context.getSourceEvent().getStream().getStreamId();

        if (poll == null) {
            messageService.send(initiatorStreamId, "You have submitted a vote for an invalid poll");
            log.info("Invalid vote cast by {} on stream {}", initiatorName, streamId);
            return;
        }

        String answer = poll.getAnswers().get(answerIndex);

        if (poll.getEnded() != null) {
            String pollEndedMsg = String.format("This poll has ended and no longer accepts votes: <i>%s</i>",
                poll.getQuestionText());
            messageService.send(initiatorStreamId, pollEndedMsg);
            log.info("Rejected vote [{}] cast by {} in stream {} on expired poll: {}",
                answer, initiatorName, streamId, poll.getQuestionText());
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
        messageService.send(dataService.getImStreamId(poll.getCreator()), creatorNotification);
    }

    @Override
    protected ActivityInfo info() {
        return new ActivityInfo().name("Respond to votes").type(ActivityType.FORM);
    }
}
