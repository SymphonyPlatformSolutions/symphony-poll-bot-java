package com.symphony.ps.pollbot.model;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import model.StreamTypes;
import org.bson.types.ObjectId;

@Data
@Builder
public class Poll {
    private ObjectId id;
    private Instant created;
    private Instant ended;
    private long creator;
    private StreamTypes type;
    private String questionText;
    private List<PollParticipant> participants;
    private List<PollAnswer> answers;
}
