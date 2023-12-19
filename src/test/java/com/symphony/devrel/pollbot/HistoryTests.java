package com.symphony.devrel.pollbot;

import static com.symphony.bdk.test.SymphonyBdkTestUtils.pushMessageToDF;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.devrel.pollbot.model.Poll;
import com.symphony.devrel.pollbot.model.PollVote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HistoryTests extends BaseTest {
    @Test
    @DisplayName("No history")
    public void noHistory() {
        mockAllMessageSends();

        pushMessageToDF(initiator, imStream, "/history", botInfo);

        verify(messages).send(eq("my-im"), contains("You have no poll history"));
    }

    @Test
    @DisplayName("No active")
    public void noActive() {
        mockAllMessageSends();

        pushMessageToDF(initiator, imStream, "/active", botInfo);

        verify(messages).send(eq("my-im"), contains("You have no active poll"));
    }

    @Test
    @DisplayName("Bad history input")
    public void badHistoryInput() {
        mockAllMessageSends();

        pushMessageToDF(initiator, imStream, "/history x", botInfo);

        verify(messages).send(eq("my-im"), contains("Usage: /history <count>"));
    }

    @Test
    @DisplayName("History")
    public void history() {
        mockAllMessageSends();
        V4Message sampleMsg = new V4Message().stream(new V4Stream().streamId("my-room"));
        when(messages.getMessage(anyString())).thenReturn(sampleMsg);

        pollRepo.save(Poll.builder()
            .id("meh")
            .creator(2L)
            .questionText("hello")
            .answers(List.of("abc", "def"))
            .created(Instant.now().minus(2, ChronoUnit.DAYS))
            .ended(Instant.now().minus(1, ChronoUnit.DAYS))
            .build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(1L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(2L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("def").userId(4L).build());

        pushMessageToDF(initiator, imStream, "/history", botInfo);

        verify(messages).send(eq("my-im"), contains("hello"));
    }

    @Test
    @DisplayName("History for 1")
    public void historyOne() {
        mockAllMessageSends();
        V4Message sampleMsg = new V4Message().stream(new V4Stream().streamId("my-room"));
        when(messages.getMessage(anyString())).thenReturn(sampleMsg);

        pollRepo.save(Poll.builder()
            .id("meh")
            .creator(2L)
            .questionText("hello")
            .answers(List.of("abc", "def"))
            .created(Instant.now().minus(2, ChronoUnit.DAYS))
            .ended(Instant.now().minus(1, ChronoUnit.DAYS))
            .build());
        pollRepo.save(Poll.builder()
            .id("unwanted")
            .creator(2L)
            .questionText("unwanted")
            .answers(List.of("qqq", "www"))
            .created(Instant.now().minus(4, ChronoUnit.DAYS))
            .ended(Instant.now().minus(3, ChronoUnit.DAYS))
            .build());

        pushMessageToDF(initiator, imStream, "/history 1", botInfo);

        verify(messages).send(eq("my-im"), not(contains("unwanted")));
    }
}
