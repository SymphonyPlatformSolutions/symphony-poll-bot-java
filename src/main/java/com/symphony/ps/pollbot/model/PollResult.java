package com.symphony.ps.pollbot.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PollResult extends PollData {
    private String answer;
    private long count;
    private int width;
}
