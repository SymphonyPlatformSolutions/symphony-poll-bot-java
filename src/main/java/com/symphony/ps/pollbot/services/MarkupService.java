package com.symphony.ps.pollbot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollBlastData;
import com.symphony.ps.pollbot.model.PollCreateData;
import com.symphony.ps.pollbot.model.PollData;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class MarkupService {
    private static ObjectMapper mapper = new ObjectMapper();
    static String pollCreateTemplate, pollBlastTemplate;

    static {
        try {
            Path pollCreatePath = Paths.get(MarkupService.class.getResource("/poll-create-form.ftl").toURI());
            pollCreateTemplate = new String(Files.readAllBytes(pollCreatePath));

            Path pollBlastPath = Paths.get(MarkupService.class.getResource("/poll-blast-form.ftl").toURI());
            pollBlastTemplate = new String(Files.readAllBytes(pollBlastPath));
        } catch (IOException | URISyntaxException e) {
            log.error("Unable to load templates", e);
        }
    }

    static String getPollCreateData(boolean showPersonSelector, int count, List<Integer> timeLimits) {
        PollCreateData pollCreateData = PollCreateData.builder()
            .count(count)
            .showPersonSelector(showPersonSelector)
            .timeLimits(timeLimits)
            .build();

        try {
            return mapper.writeValueAsString(wrapData(pollCreateData));
        } catch (JsonProcessingException e) {
            log.error("Unable to write poll create data object as string", e);
            return null;
        }
    }

    static String getPollBlastData(Poll poll) {
        PollBlastData pollBlastData = PollBlastData.builder()
            .id(poll.getId().toString())
            .question(poll.getQuestionText())
            .answers(poll.getAnswers())
            .timeLimit(poll.getTimeLimit())
            .creatorId(poll.getCreator())
            .build();

        try {
            return mapper.writeValueAsString(wrapData(pollBlastData));
        } catch (JsonProcessingException e) {
            log.error("Unable to write poll blast data object as string", e);
            return null;
        }
    }

    private static Map<String, PollData> wrapData(PollData data) {
        Map<String, PollData> map = new HashMap<>();
        map.put("poll", data);
        return map;
    }
}
