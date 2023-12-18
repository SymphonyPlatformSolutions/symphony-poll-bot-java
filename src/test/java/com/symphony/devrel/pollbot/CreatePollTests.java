package com.symphony.devrel.pollbot;

import static com.symphony.bdk.test.SymphonyBdkTestUtils.pushMessageToDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import java.util.List;
import java.util.Map;

public class CreatePollTests extends BaseTest {
    @Test
    @DisplayName("Create poll contains too little options")
    public void tooLittleOptions() {
        mockAllMessageSends();

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc"
        ));
        verify(messages).send(eq("my-im"), contains("Your poll contains less than 2 valid options"));
    }

    @Test
    @DisplayName("Invalid target stream")
    public void invalidTargetStream() {
        mockAllMessageSends();

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "targetStreamId", "room-stream"
        ));

        verify(messages).send(eq("my-im"), contains("Your room stream id is invalid"));
    }

    @Test
    @DisplayName("Not a member of target stream")
    public void notMemberOfTargetStream() {
        mockAllMessageSends();
        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail());
        when(streams.listRoomMembers(anyString())).thenReturn(null);

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "targetStreamId", "room-stream"
        ));

        verify(messages).send(eq("my-im"), contains("I am not a member in that room"));
    }

    @Test
    @DisplayName("Read-only stream")
    public void readOnlyStream() {
        mockAllMessageSends();
        when(streams.getRoomInfo(anyString()))
            .thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(true)));
        when(streams.listRoomMembers(anyString())).thenReturn(List.of());

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "targetStreamId", "room-stream"
        ));

        verify(messages).send(eq("my-im"), contains("Room is read-only"));
    }

    @Test
    @DisplayName("Invalid time limit")
    public void invalidTimeLimit() {
        mockAllMessageSends();
        when(streams.getRoomInfo(anyString()))
            .thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.listRoomMembers(anyString())).thenReturn(List.of());

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "targetStreamId", "room-stream",
            "timeLimit", "blah"
        ));

        verify(messages).send(eq("my-im"), contains("Expiry should be a number"));
    }

    @Test
    @DisplayName("Time limit too long")
    public void timeLimitTooLong() {
        mockAllMessageSends();
        when(streams.getRoomInfo(anyString()))
            .thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.listRoomMembers(anyString())).thenReturn(List.of());

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "targetStreamId", "room-stream",
            "timeLimit", "1441"
        ));

        verify(messages).send(eq("my-im"), contains("Expiry should be between 0 and 1440"));
    }

    @Captor
    ArgumentCaptor<Message> messageCaptor;

    @Test
    @DisplayName("Blast to room")
    public void blastRoom() {
        mockAllMessageSends();
        when(streams.getRoomInfo(anyString()))
            .thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.listRoomMembers(anyString())).thenReturn(List.of());

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "targetStreamId", "room-stream",
            "timeLimit", "2"
        ));

        verify(messages).send(eq("room-stream"), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent())
            .contains("Poll: my poll question by")
            .contains("will end in 2 minutes");

        verify(messages).send(eq("my-im"), contains("Your poll has been created"));

        pushMessageToDF(initiator, roomStream, "/poll", botInfo);
        verify(messages).send(eq("my-im"), contains("You already have an existing active poll"));
    }

    @Test
    @DisplayName("Blast to IMs")
    public void blastIMs() {
        mockAllMessageSends();

        pushElementsAction("poll-create-form", Map.of(
            "question", "my poll question",
            "option-1", "abc",
            "option-2", "def",
            "audience", List.of(123L),
            "timeLimit", "2"
        ));

        verify(messages).send(eq(List.of("my-im")), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent())
            .contains("Poll: my poll question by")
            .contains("will end in 2 minutes");

        verify(messages).send(eq("my-im"), contains("Your poll has been created"));

        pushMessageToDF(initiator, imStream, "/poll", botInfo);
        verify(messages).send(eq("my-im"), contains("You already have an existing active poll"));
    }
}
