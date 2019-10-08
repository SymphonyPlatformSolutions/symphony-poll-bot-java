package com.symphony.ps.pollbot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.symphony.ps.pollbot.model.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
        PollCreateObject pollCreateObject = PollCreateObject.builder().newPoll(
            PollCreateData.builder()
                .count(count)
                .showPersonSelector(showPersonSelector)
                .timeLimits(timeLimits)
                .build()
        ).build();

        try {
            return mapper.writeValueAsString(pollCreateObject);
        } catch (JsonProcessingException e) {
            log.error("Unable to write poll create data object as string", e);
            return null;
        }
    }

    static String getPollBlastData(Poll poll) {
        PollBlastObject pollBlastObject = PollBlastObject.builder()
            .poll(
                PollBlastData.builder()
                    .id(poll.getId().toString())
                    .question(poll.getQuestionText())
                    .answers(poll.getAnswers())
                    .timeLimit(poll.getTimeLimit())
                    .creatorId(poll.getCreator())
                    .build()
            )
            .build();

        try {
            return mapper.writeValueAsString(pollBlastObject);
        } catch (JsonProcessingException e) {
            log.error("Unable to write poll blast data object as string", e);
            return null;
        }
    }
}
