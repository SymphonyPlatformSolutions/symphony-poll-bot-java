package com.symphony.ps.pollbot.model;

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
public class Poll {
    private String id;
    private Instant created;
    private Instant ended;
    private int timeLimit;
    private long creator;
    private String streamId;
    private String questionText;
    private List<PollParticipant> participants;
    private List<String> answers;
}
