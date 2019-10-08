package com.symphony.ps.pollbot.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewPollObject {
    private NewPollData newPoll;
}
