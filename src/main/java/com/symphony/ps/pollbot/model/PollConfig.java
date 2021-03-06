package com.symphony.ps.pollbot.model;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PollConfig {
    private int options;
    private List<Integer> timeLimits;
    private boolean targetStream;

    public PollConfig() {
        options = 6;
        timeLimits = Arrays.asList(0, 2, 5);
    }
}
