package com.symphony.ps.pollbot.listeners;

import com.symphony.ps.pollbot.services.PollService;
import listeners.RoomListener;
import model.InboundMessage;
import model.Stream;
import model.StreamTypes;
import model.events.*;

public class RoomListenerImpl implements RoomListener {
    public void onRoomMessage(InboundMessage message) {
        PollService.handleIncomingMessage(message, StreamTypes.ROOM);
    }

    public void onRoomCreated(RoomCreated roomCreated) {}
    public void onRoomDeactivated(RoomDeactivated roomDeactivated) {}
    public void onRoomMemberDemotedFromOwner(RoomMemberDemotedFromOwner roomMemberDemotedFromOwner) {}
    public void onRoomMemberPromotedToOwner(RoomMemberPromotedToOwner roomMemberPromotedToOwner) {}
    public void onRoomReactivated(Stream stream) {}
    public void onRoomUpdated(RoomUpdated roomUpdated) {}
    public void onUserJoinedRoom(UserJoinedRoom userJoinedRoom) {}
    public void onUserLeftRoom(UserLeftRoom userLeftRoom) {}
}
