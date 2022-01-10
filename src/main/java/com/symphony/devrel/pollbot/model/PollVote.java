package com.symphony.devrel.pollbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {
    private String id;
    private String pollId;
    private String answer;
    private long userId;
}
