package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PollHistory extends PollData {
    private String creatorId;
    private boolean room;
    private List<PollDTO> polls;
}
