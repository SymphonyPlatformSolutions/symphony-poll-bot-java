package com.symphony.ps.pollbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PollParticipant {
    private long userId;
    private String imStreamId;
}
