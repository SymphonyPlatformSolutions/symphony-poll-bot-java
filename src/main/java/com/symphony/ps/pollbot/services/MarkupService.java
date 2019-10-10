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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    static String getPollCreateData(boolean showPersonSelector, int count, List<Integer> timeLimits) {
        return wrapData(new PollCreateData(count, timeLimits, showPersonSelector));
    }

    static String getPollBlastData(Poll poll) {
        return wrapData(new PollBlastData(
            poll.getId().toString(),
            poll.getTimeLimit(),
            poll.getQuestionText(),
            poll.getAnswers(),
            poll.getCreator()
        ));
    }

    static String wrapData(PollData data) {
        Map<String, PollData> map = new HashMap<>();
        map.put("poll", data);

        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Unable to wrap data object", e);
            return null;
        }
    }
}
