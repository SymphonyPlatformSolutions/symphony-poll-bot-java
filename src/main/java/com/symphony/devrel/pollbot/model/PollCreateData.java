package com.symphony.devrel.pollbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Builder
public class PollCreateData extends PollData {
    private boolean showPersonSelector;
    private String targetStreamId;
    private int count;
}
