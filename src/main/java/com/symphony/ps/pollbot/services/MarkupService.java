package com.symphony.ps.pollbot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.symphony.ps.pollbot.model.NewPollData;
import com.symphony.ps.pollbot.model.NewPollObject;
import com.symphony.ps.pollbot.model.Poll;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.FormButtonType;
import utils.FormBuilder;

@Slf4j
class MarkupService {
    public static String newPollTemplate;
    private static ObjectMapper mapper = new ObjectMapper();

    static {
/*        try {
            newPollTemplate = mapper.readValue(MarkupService.class.getResourceAsStream("/new-poll.ftl"), String.class);
        } catch (IOException e) {
            log.error("Unable to load new poll template", e);
        }*/
        try {
            newPollTemplate = new String(Files.readAllBytes(Paths.get(MarkupService.class.getResource("/new-poll.ftl").toURI())));
        } catch (IOException | URISyntaxException e) {
            log.error("Unable to load new poll template", e);
        }
    }

    static String getNewPollData(boolean showPersonSelector, int count, List<Integer> timeLimits) {
        NewPollData data = NewPollData.builder()
            .count(count)
            .showPersonSelector(showPersonSelector)
            .timeLimits(timeLimits)
            .build();
        NewPollObject newPollObject = NewPollObject.builder().newPoll(data).build();
        try {
            return mapper.writeValueAsString(newPollObject);
        } catch (JsonProcessingException e) {
            log.error("Unable to write new poll data object as string", e);
            return null;
        }
    }

    static String getBlastPollML(Poll poll) {
        FormBuilder formBuilder = FormBuilder.builder("poll-blast-form-" + poll.getId())
            .addHeader(6, String.format("Poll by <mention uid=\"%d\" />", poll.getCreator()))
            .addHeader(6, poll.getQuestionText());
        int index = 0;
        for (String answer : poll.getAnswers()) {
            formBuilder.addButton(
                "option-" + index++, answer, FormButtonType.ACTION
            );
        }

        String timeLimitNote = poll.getTimeLimit() == 0 ? "does not have a time limit"
            : String.format("will end in %d minutes", poll.getTimeLimit());
        formBuilder.addDiv(String.format("<i>This poll %s</i>", timeLimitNote));

        return formBuilder.formatElement();
    }
}
