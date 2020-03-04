package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PollResultsData extends PollData {
    private String question;
    private String creatorDisplayName;
    private List<PollResult> results;
}
