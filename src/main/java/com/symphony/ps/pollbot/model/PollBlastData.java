package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class PollBlastData extends PollData {
    private String id;
    private int timeLimit;
    private String question;
    private List<String> answers;
    private String creatorDisplayName;
}
