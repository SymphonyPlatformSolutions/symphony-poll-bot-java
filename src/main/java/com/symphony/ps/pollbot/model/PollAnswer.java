package com.symphony.ps.pollbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PollAnswer {
    private long answerId;
    private String answerText;
}
