package com.symphony.devrel.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PollHistory extends PollData {
    private String creatorDisplayName;
    private boolean room;
    private List<PollHistoryItem> polls;
}
