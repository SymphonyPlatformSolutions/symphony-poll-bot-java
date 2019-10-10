package com.symphony.ps.pollbot.services;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollResult;
import com.symphony.ps.pollbot.model.PollVote;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Slf4j
public class DataService {
    @Getter
    MongoCollection<Poll> pollCollection;
    @Getter
    MongoCollection<PollVote> voteCollection;

    public DataService(String url) {
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

    boolean hasActivePoll(long userId) {
        return 1L == pollCollection.countDocuments(
            and(
                eq("creator", userId),
                eq("ended", null)
            )
        );
    }

    void createPoll(Poll poll) {
        pollCollection.insertOne(poll);
        log.info("Poll added to database: {}", poll.toString());
    }

    void endPoll(long userId) {
        pollCollection.updateOne(and(
            eq("creator", userId),
            eq("ended", null)
        ), set("ended", Instant.now()));
    }

    Poll getPoll(String id) {
        return pollCollection.find(eq("_id", new ObjectId(id))).first();
    }

    Poll getActivePoll(long userId) {
        return pollCollection.find(
            and(
                eq("creator", userId),
                eq("ended", null)
            )
        ).first();
    }

    List<Poll> getPolls(long userId) {
        return pollCollection
            .find(eq("creator", userId))
            .sort(ascending("created"))
            .into(new ArrayList<>());
    }

    List<Poll> getPolls(long userId, String streamId) {
        return pollCollection
            .find(and(
                eq("creator", userId),
                eq("streamId", streamId)
            ))
            .sort(ascending("created"))
            .into(new ArrayList<>());
    }

    List<PollVote> getVotes(ObjectId pollId) {
        return voteCollection.find(
            eq("pollId", pollId)
        ).into(new ArrayList<>());
    }

    List<PollResult> getPollResults(ObjectId pollId) {
        return voteCollection.aggregate(
            Arrays.asList(
                match(eq("pollId", pollId)),
                group("$answer", sum("count", 1)),
                sort(descending("count")),
                project(fields(
                    computed("answer", "$_id"),
                    include("count")
                ))
            ), PollResult.class
        ).into(new ArrayList<>());
    }

    void createVote(PollVote vote) {
        voteCollection.insertOne(vote);
        log.info("Vote added to database: {}", vote.toString());
    }

    void createVotes(List<PollVote> votes) {
        voteCollection.insertMany(votes);
        log.info("Rigged votes added to database");
    }

    boolean hasVoted(long userId, String pollId) {
        return 1L == voteCollection.countDocuments(and(
            eq("pollId", new ObjectId(pollId)),
            eq("userId", userId)
        ));
    }

    void changeVote(long userId, String pollId, String answer) {
        voteCollection.updateOne(and(
            eq("pollId", new ObjectId(pollId)),
            eq("userId", userId)
        ), set("answer", answer));
    }
}
