package com.symphony.devrel.pollbot.model;

import com.symphony.bdk.gen.api.model.StreamType.TypeEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageMetadata {
    private long userId;
    private String displayName;
    private String streamId;
    private TypeEnum streamType;
}
