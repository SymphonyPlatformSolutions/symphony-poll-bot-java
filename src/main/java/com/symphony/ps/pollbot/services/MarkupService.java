package com.symphony.ps.pollbot.services;

import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollBlastData;
import com.symphony.ps.pollbot.model.PollCreateData;
import com.symphony.ps.pollbot.model.PollData;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
class MarkupService {

    static String pollCreateTemplate = "/poll-create-form.ftl";
    static String pollBlastTemplate = "/poll-blast-form.ftl";
    static String pollResultsTemplate = "/poll-results.ftl";
    static String pollResultsEmptyTemplate = "/poll-results-empty.ftl";
    static String pollHistoryTemplate = "/poll-history.ftl";

    static Map<String, PollData> getPollCreateData(boolean showPersonSelector, String targetStreamId, int count, List<Integer> timeLimits) {
        return Collections.singletonMap("data", new PollCreateData(
                showPersonSelector,
                targetStreamId,
                count,
                timeLimits));
    }

    static Map<String, PollData> getPollBlastData(Poll poll) {
        return Collections.singletonMap("data", new PollBlastData(
                poll.getId() + "",
                poll.getTimeLimit(),
                poll.getQuestionText(),
                poll.getAnswers(),
                poll.getCreatorDisplayName()
        ));
    }
}
