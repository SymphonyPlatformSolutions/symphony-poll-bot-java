package com.symphony.ps.pollbot.model;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class PollVote {
    private ObjectId voteId;
    private ObjectId pollId;
    private ObjectId answerId;
    private long userId;
}
