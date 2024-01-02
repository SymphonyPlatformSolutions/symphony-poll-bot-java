package com.symphony.devrel.pollbot;

import static com.symphony.bdk.test.SymphonyBdkTestUtils.pushEventToDataFeed;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4Event;
import com.symphony.bdk.gen.api.model.V4Initiator;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageBlastResponse;
import com.symphony.bdk.gen.api.model.V4Payload;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.bdk.test.SymphonyBdkTestUtils;
import com.symphony.bdk.test.spring.annotation.SymphonyBdkSpringBootTest;
import com.symphony.devrel.pollbot.repository.PollRepository;
import com.symphony.devrel.pollbot.repository.PollVoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@SymphonyBdkSpringBootTest
public class BaseTest {
    @Autowired
    MessageService messages;

    @Autowired
    StreamService streams;

    @Autowired
    UserV2 botInfo;

    @Autowired
    PollRepository pollRepo;

    @Autowired
    PollVoteRepository pollVoteRepo;

    final V4User initiator = new V4User().displayName("user").userId(2L);
    final V4Stream imStream = new V4Stream().streamType("IM").streamId("my-im");
    final V4Stream roomStream = new V4Stream().streamType("ROOM").streamId("my-room");

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:6").withReuse(true);

    static {
        mongodb.start();
    }

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    @AfterEach
    void afterEach() {
        pollRepo.deleteAll();
        pollVoteRepo.deleteAll();
    }

    void mockAllMessageSends() {
        V4Message msg = new V4Message().messageId("abc");
        when(messages.send(anyString(), any(Message.class))).thenReturn(msg);
        when(messages.send(anyString(), any(String.class))).thenReturn(msg);
        when(messages.send(anyList(), any(Message.class))).thenReturn(new V4MessageBlastResponse().messages(List.of(msg)));
        when(streams.create(anyLong())).thenReturn(new Stream().id("my-im"));
    }

    void pushElementsAction(String formId, Map<String, Object> payload) {
        V4SymphonyElementsAction action = new V4SymphonyElementsAction()
            .formId(formId)
            .formMessageId("form-message-id")
            .formValues(payload)
            .stream(imStream);
        pushEventToDataFeed(new V4Event().id("id").timestamp(Instant.now().toEpochMilli())
            .initiator(new V4Initiator().user(initiator))
            .payload(new V4Payload().symphonyElementsAction(action))
            .type(SymphonyBdkTestUtils.V4EventType.SYMPHONYELEMENTSACTION.name()));
    }
}
