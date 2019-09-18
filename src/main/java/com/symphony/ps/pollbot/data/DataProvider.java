package com.symphony.ps.pollbot.data;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollVote;
import java.time.Instant;
import java.util.Collection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Slf4j
public class DataProvider {
    @Getter
    MongoCollection<Poll> pollCollection;
    @Getter
    MongoCollection<PollVote> voteCollection;

    public DataProvider(String url) {
        CodecRegistry registry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        MongoClient mongoClient = MongoClients.create(
            MongoClientSettings.builder()
                .codecRegistry(registry)
                .applyConnectionString(new ConnectionString(url))
                .build()
        );
        MongoDatabase db = mongoClient.getDatabase("PollBot");

        pollCollection = db.getCollection("poll", Poll.class);
        voteCollection = db.getCollection("vote", PollVote.class);
    }

    public boolean hasActivePoll(long userId) {
        return 1L == pollCollection.countDocuments(
            and(
                eq("creator", userId),
                eq("ended", null)
            )
        );
    }

    public void createPoll(Poll poll) {
        pollCollection.insertOne(poll);
        log.info("Poll added to database: {}", poll.toString());
    }

    public void endPoll(long userId) {
        pollCollection.updateOne(and(
            eq("creator", userId),
            eq("ended", null)
        ), set("ended", Instant.now()));
    }

    public Poll getPoll(String id) {
        return pollCollection.find(eq("_id", new ObjectId(id))).first();
    }

    public void createVote(PollVote vote) {
        voteCollection.insertOne(vote);
        log.info("Vote added to database: {}", vote.toString());
    }

    public boolean hasVoted(long userId, String pollId) {
        return 1L == voteCollection.countDocuments(
            and(
                eq("pollId", new ObjectId(pollId)),
                eq("userId", userId)
            )
        );
    }

    public void changeVote(long userId, String pollId, String answer) {
        pollCollection.updateOne(and(
            eq("pollId", new ObjectId(pollId)),
            eq("userId", userId)
        ), set("answer", answer));
    }
}
