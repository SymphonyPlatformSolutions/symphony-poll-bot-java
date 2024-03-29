package com.symphony.devrel.pollbot.model;

import java.util.Objects;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollResult extends PollData {
    private String pollId;
    private String answer;
    private long count;
    private int width;

    public PollResult(String answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "{" + answer + "=" + count + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof PollResult)) { return false; }
        PollResult that = (PollResult) o;
        return answer.equals(that.answer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer);
    }
}
