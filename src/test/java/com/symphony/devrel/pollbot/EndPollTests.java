package com.symphony.devrel.pollbot;

import static com.symphony.bdk.test.SymphonyBdkTestUtils.pushMessageToDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.devrel.pollbot.model.Poll;
import com.symphony.devrel.pollbot.model.PollVote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EndPollTests extends BaseTest {
    @Test
    @DisplayName("Deprecated warning")
    void deprecatedWarning() {
        mockAllMessageSends();

        pushMessageToDF(initiator, roomStream, "/endpoll");
        verify(messages).send(eq("my-im"), contains("@mention prefix is now required"));
    }

    @Test
    @DisplayName("No active poll")
    void noActivePoll() {
        mockAllMessageSends();

        pushMessageToDF(initiator, roomStream, "/endpoll", botInfo);
        verify(messages).send(eq("my-im"), contains("You have no active poll to end"));
    }

    @Test
    @DisplayName("Empty poll")
    void emptyPoll() {
        mockAllMessageSends();

        pollRepo.save(Poll.builder()
            .id("meh")
            .creator(2L)
            .answers(List.of("abc", "def"))
            .streamId("my-im")
            .build());

        pushMessageToDF(initiator, roomStream, "/endpoll", botInfo);
        verify(messages).send(eq("my-im"), contains("Poll ended but with no results to show"));
    }

    @Captor
    ArgumentCaptor<String> stringCaptor;

    @Test
    @DisplayName("Participated poll")
    void participatedPoll() {
        mockAllMessageSends();

        pollRepo.save(Poll.builder()
            .id("meh")
            .creator(2L)
            .creatorDisplayName("John")
            .questionText("question")
            .answers(List.of("abc", "def", "ghi", "jkl"))
            .streamId("my-room")
            .build());

        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(1L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(2L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(3L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("def").userId(4L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("def").userId(5L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("ghi").userId(6L).build());

        pushMessageToDF(initiator, roomStream, "/endpoll", botInfo);

        verify(messages).send(eq("my-room"), stringCaptor.capture());
        assertThat(stringCaptor.getValue())
            .contains("Poll Results: question by John")
            .contains("text-align:right\">3")
            .contains("text-align:right\">2")
            .contains("text-align:right\">1")
            .contains("text-align:right\">0");
    }

    @Captor
    ArgumentCaptor<Message> messageCaptor;

    @Test
    @DisplayName("Participated poll update")
    void participatedPollUpdate() {
        mockAllMessageSends();
        V4Message sampleMsg = new V4Message().stream(new V4Stream().streamId("my-room"));
        when(messages.getMessage(anyString())).thenReturn(sampleMsg);

        pollRepo.save(Poll.builder()
            .id("meh")
            .creator(2L)
            .creatorDisplayName("John")
            .questionText("question")
            .answers(List.of("abc", "def", "ghi", "jkl"))
            .streamId("my-room")
            .messageIds(List.of("abc"))
            .statusMessageId("abc")
            .build());

        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(1L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(2L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("abc").userId(3L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("def").userId(4L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("def").userId(5L).build());
        pollVoteRepo.save(PollVote.builder().pollId("meh").answer("ghi").userId(6L).build());

        pushMessageToDF(initiator, roomStream, "/endpoll", botInfo);

        verify(messages, times(2)).update(eq("my-room"), eq("abc"), messageCaptor.capture());
        String contents = messageCaptor.getAllValues().stream()
            .map(Message::getContent).collect(Collectors.joining());
        assertThat(contents)
            .contains("You have ended the poll")
            .contains("This poll has ended");

        verify(messages).send(eq("my-room"), stringCaptor.capture());
        assertThat(stringCaptor.getValue())
            .contains("Poll Results: question by John")
            .contains("text-align:right\">3")
            .contains("text-align:right\">2")
            .contains("text-align:right\">1")
            .contains("text-align:right\">0");
    }

    @Test
    @DisplayName("End poll form")
    public void endPollForm() {
        mockAllMessageSends();

        pushElementsAction("poll-end-form", Map.of());

        verify(messages).send(eq("my-im"), contains("You have no active poll to end"));
    }
}
