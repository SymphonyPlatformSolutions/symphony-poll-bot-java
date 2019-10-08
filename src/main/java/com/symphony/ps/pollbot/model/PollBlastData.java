package com.symphony.ps.pollbot.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PollBlastData {
    private String id;
    private int timeLimit;
    private String question;
    private List<String> answers;
    private long creatorId;

    public String getCreatorId() {
        return creatorId + "";
    }
}
