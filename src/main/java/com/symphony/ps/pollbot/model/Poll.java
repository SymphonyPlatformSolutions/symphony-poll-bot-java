package com.symphony.ps.pollbot.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Poll {
    private ObjectId id;
    private Instant created;
    private Instant ended;
    private int timeLimit;
    private long creator;
    private String streamId;
    private String questionText;
    private List<PollParticipant> participants;
    private List<String> answers;
}
