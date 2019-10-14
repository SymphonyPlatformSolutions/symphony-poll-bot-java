package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PollHistory extends PollData {
    private long creatorId;
    private boolean room;
    private List<PollHistoryItem> polls;

    public String getCreatorId() {
        return creatorId + "";
    }
}
