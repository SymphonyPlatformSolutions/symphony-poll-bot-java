package com.symphony.ps.pollbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {
    private ObjectId id;
    private ObjectId pollId;
    private String answer;
    private long userId;
}
