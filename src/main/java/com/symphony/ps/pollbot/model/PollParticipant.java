package com.symphony.ps.pollbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollParticipant {
    private long userId;
    private String imStreamId;
}
