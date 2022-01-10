package com.symphony.devrel.pollbot.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class PollBlastData extends PollData {
    private String id;
    private int timeLimit;
    private String question;
    private List<String> answers;
    private String creatorDisplayName;
}
