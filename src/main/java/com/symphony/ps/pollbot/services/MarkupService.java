package com.symphony.ps.pollbot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollBlastData;
import com.symphony.ps.pollbot.model.PollCreateData;
import com.symphony.ps.pollbot.model.PollData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class MarkupService {
    private static ObjectMapper mapper = new ObjectMapper();
    static String pollCreateTemplate = loadTemplate("/poll-create-form.ftl");
    static String pollBlastTemplate = loadTemplate("/poll-blast-form.ftl");
    static String pollResultsTemplate = loadTemplate("/poll-results.ftl");
    static String pollHistoryTemplate = loadTemplate("/poll-history.ftl");

    private static String loadTemplate(String fileName) {
        InputStream stream = MarkupService.class.getResourceAsStream(fileName);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        catch (IOException e) {
            log.error("Unable to load template for {}", fileName);
            return null;
        }
    }

    static String getPollCreateData(boolean showPersonSelector, String targetStreamId, int count, List<Integer> timeLimits) {
        return convertToJsonString(new PollCreateData(showPersonSelector, targetStreamId, count, timeLimits));
    }

    static String getPollBlastData(Poll poll) {
        return convertToJsonString(new PollBlastData(
            poll.getId() + "",
            poll.getTimeLimit(),
            poll.getQuestionText(),
            poll.getAnswers(),
            poll.getCreatorDisplayName()
        ));
    }

    static String convertToJsonString(PollData data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Unable to wrap data object: {}", e.getMessage());
            return null;
        }
    }
}
