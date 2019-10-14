package com.symphony.ps.pollbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollDTO {
    private Instant created;
    private Instant ended;
    private String questionText;
    private List<String> answers;

    public static PollDTO fromPoll(Poll poll) {
        return PollDTO.builder()
            .created(poll.getCreated())
            .ended(poll.getEnded())
            .questionText(poll.getQuestionText())
            .answers(poll.getAnswers())
            .build();
    }
}
