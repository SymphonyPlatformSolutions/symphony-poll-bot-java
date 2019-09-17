package com.symphony.ps.pollbot.model;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class PollAnswer {
    private ObjectId answerId;
    private String answerText;

    public PollAnswer(String answerText) {
        this.answerText = answerText;
    }
}
