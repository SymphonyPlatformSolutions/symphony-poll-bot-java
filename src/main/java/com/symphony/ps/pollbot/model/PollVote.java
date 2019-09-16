package com.symphony.ps.pollbot.model;

import lombok.Data;

@Data
public class PollVote {
    private long voteId;
    private long pollId;
    private long answerId;
    private long userId;
}
