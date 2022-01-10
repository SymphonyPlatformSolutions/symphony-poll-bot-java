package com.symphony.devrel.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PollResultsData extends PollData {
    private String question;
    private String creatorDisplayName;
    private List<PollResult> results;
}
