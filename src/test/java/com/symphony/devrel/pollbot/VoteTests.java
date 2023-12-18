package com.symphony.devrel.pollbot;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import com.symphony.devrel.pollbot.model.Poll;
import com.symphony.devrel.pollbot.model.PollVote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class VoteTests extends BaseTest {
    @Test
    @DisplayName("Invalid vote")
    public void invalidVote() {
        mockAllMessageSends();

        pushElementsAction("poll-blast-form-meh", Map.of(
            "action", "option-1"
        ));

        verify(messages).send(eq("my-im"), contains("You have submitted a vote for an invalid poll"));
    }

    @Test
    @DisplayName("Poll ended")
    public void pollEnded() {
        mockAllMessageSends();

        pollRepo.save(Poll.builder()
            .id("meh")
            .answers(List.of("abc", "def"))
            .ended(Instant.now())
            .build());

        pushElementsAction("poll-blast-form-meh", Map.of(
            "action", "option-1"
        ));

        verify(messages).send(eq("my-im"), contains("This poll has ended and no longer accepts votes"));
    }

    @Test
    @DisplayName("Vote")
    public void vote() {
        mockAllMessageSends();

        pollRepo.save(Poll.builder()
            .id("meh")
            .answers(List.of("abc", "def"))
            .build());

        pushElementsAction("poll-blast-form-meh", Map.of(
            "action", "option-1"
        ));

        verify(messages).send(eq("my-im"), contains("Thanks for voting <b>def</b>"));
    }

    @Test
    @DisplayName("Update vote")
    public void updateVote() {
        mockAllMessageSends();

        pollRepo.save(Poll.builder()
            .id("meh")
            .answers(List.of("abc", "def"))
            .build());

        pollVoteRepo.save(PollVote.builder()
            .pollId("meh")
            .answer("abc")
            .userId(2L)
            .build());

        pushElementsAction("poll-blast-form-meh", Map.of(
            "action", "option-1"
        ));

        verify(messages).send(eq("my-im"), contains("Your vote has been updated to <b>def</b>"));
    }
}
