package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PollResultsData extends PollData {
    private String question;
    private long creatorId;
    private List<PollResult> results;

    public String getCreatorId() {
        return creatorId + "";
    }
}
