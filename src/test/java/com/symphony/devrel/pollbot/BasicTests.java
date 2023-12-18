package com.symphony.devrel.pollbot;

import static com.symphony.bdk.test.SymphonyBdkTestUtils.pushMessageToDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.symphony.bdk.gen.api.model.MemberInfo;
import com.symphony.bdk.gen.api.model.V1IMAttributes;
import com.symphony.bdk.gen.api.model.V1IMDetail;
import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.gen.api.model.V2StreamType;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import java.util.List;

public class BasicTests extends BaseTest {
    @Test
    @DisplayName("Help command for IM and rooms")
    void help() {
        mockAllMessageSends();

        pushMessageToDF(initiator, imStream, "/help", botInfo);
        verify(messages).send(eq("my-im"), contains("Preview the results of your active poll<"));

        pushMessageToDF(initiator, roomStream, "/help", botInfo);
        verify(messages).send(eq("my-room"), contains("Preview the results of your active poll for this room<"));
    }

    @Test
    @DisplayName("Get create poll form - Read only")
    void readOnly() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(true)));

        pushMessageToDF(initiator, roomStream, "/poll", botInfo);
        verify(messages).send(eq("my-im"), contains("Unable to create poll in a read-only room"));
    }

    @Test
    @DisplayName("Get create poll form")
    void sendPollForm() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));

        pushMessageToDF(initiator, roomStream, "/poll", botInfo);
        verify(messages).send(eq("my-im"), contains("Create New Poll"));
    }

    @Test
    @DisplayName("Get create poll form - with invalid stream id")
    void sendPollFormWithInvalidStreamId() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));

        pushMessageToDF(initiator, imStream, "/poll abcdefg", botInfo);
        verify(messages).send(eq("my-im"), contains("Invalid stream id"));
    }

    @Test
    @DisplayName("Get create poll form - with stream id")
    void sendPollFormWithStreamId() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.getStream(anyString())).thenReturn(new V2StreamAttributes().streamType(new V2StreamType().type("ROOM")));

        pushMessageToDF(initiator, imStream, "/poll abcdefg", botInfo);
        verify(messages).send(eq("my-im"), contains("Create New Poll"));
    }

    @Test
    @DisplayName("Get create poll form - with below minimum count")
    void sendPollFormWithBelowMinCount() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.getStream(anyString())).thenReturn(new V2StreamAttributes().streamType(new V2StreamType().type("ROOM")));

        pushMessageToDF(initiator, imStream, "/poll 1", botInfo);
        verify(messages).send(eq("my-im"), contains("Options should be between 2 and 10"));
    }

    @Test
    @DisplayName("Get create poll form - with below minimum count")
    void sendPollFormWithAboveMaxCount() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.getStream(anyString())).thenReturn(new V2StreamAttributes().streamType(new V2StreamType().type("ROOM")));

        pushMessageToDF(initiator, imStream, "/poll 11", botInfo);
        verify(messages).send(eq("my-im"), contains("Options should be between 2 and 10"));
    }

    @Test
    @DisplayName("Get create poll form - with custom count")
    void sendPollFormWithCustomCount() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.getStream(anyString())).thenReturn(new V2StreamAttributes().streamType(new V2StreamType().type("ROOM")));

        pushMessageToDF(initiator, imStream, "/poll 5", botInfo);
        verify(messages).send(eq("my-im"), contains("Create New Poll"));
    }

    @Test
    @DisplayName("Deprecated warning")
    void deprecatedWarning() {
        mockAllMessageSends();

        pushMessageToDF(initiator, roomStream, "/poll");
        verify(messages).send(eq("my-im"), contains("@mention prefix is now required"));
    }

    @Captor
    ArgumentCaptor<V3RoomAttributes> roomAttributesCaptor;

    @Test
    @DisplayName("Pin poll form - room")
    void pinPollFormRoom() {
        mockAllMessageSends();

        when(streams.getRoomInfo(anyString())).thenReturn(new V3RoomDetail().roomAttributes(new V3RoomAttributes().readOnly(false)));
        when(streams.listRoomMembers(anyString())).thenReturn(List.of(new MemberInfo().id(1L).owner(true)));

        pushMessageToDF(initiator, roomStream, "/pin", botInfo);
        verify(messages).send(eq("my-room"), contains("Create New Poll"));
        verify(streams).updateRoom(eq("my-room"), roomAttributesCaptor.capture());
        assertThat(roomAttributesCaptor.getValue().getPinnedMessageId())
            .isEqualTo("abc");
    }

    @Captor
    ArgumentCaptor<V1IMAttributes> imAttributesCaptor;

    @Test
    @DisplayName("Pin poll form - IM")
    void pinPollFormIm() {
        mockAllMessageSends();

        when(streams.getInstantMessageInfo(anyString())).thenReturn(new V1IMDetail().v1IMAttributes(new V1IMAttributes()));

        pushMessageToDF(initiator, imStream, "/pin", botInfo);
        verify(messages).send(eq("my-im"), contains("Create New Poll"));
        verify(streams).updateInstantMessage(eq("my-im"), imAttributesCaptor.capture());
        assertThat(imAttributesCaptor.getValue().getPinnedMessageId())
            .isEqualTo("abc");
    }
}
