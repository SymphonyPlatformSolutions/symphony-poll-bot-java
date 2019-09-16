package com.symphony.ps.pollbot.data;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollVote;
import lombok.Getter;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

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
}
