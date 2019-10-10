package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollDTO {
    private String created;
    private String ended;
    private String questionText;
    private List<String> answers;

    public static PollDTO fromPoll(Poll poll) {
        return PollDTO.builder()
            .created(poll.getCreated().toString())
            .ended(poll.getEnded().toString())
            .questionText(poll.getQuestionText())
            .answers(poll.getAnswers())
            .build();
    }
}
