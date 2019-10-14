package com.symphony.ps.pollbot.services;

import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollResult;
import com.symphony.ps.pollbot.model.PollVote;
import com.symphony.ps.pollbot.repository.PollRepository;
import com.symphony.ps.pollbot.repository.PollVoteRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j
@Service
public class DataService {
    private final MongoTemplate mongoTemplate;
    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;

    public DataService(MongoTemplate mongoTemplate, PollRepository pollRepository, PollVoteRepository pollVoteRepository) {
        this.mongoTemplate = mongoTemplate;
        this.pollRepository = pollRepository;
        this.pollVoteRepository = pollVoteRepository;
    }

    boolean hasActivePoll(long userId) {
        return 1L == pollRepository.countByCreatorAndEnded(userId, null);
    }

    void createPoll(Poll poll) {
        pollRepository.save(poll);
        log.info("Poll added to database: {}", poll.toString());
    }

    void endPoll(long userId) {
        Poll poll = getActivePoll(userId);
        poll.setEnded(Instant.now());
        pollRepository.save(poll);
    }

    Poll getPoll(String id) {
        return pollRepository.findById(id).orElse(null);
    }

    Poll getActivePoll(long userId) {
        return pollRepository.findTopByCreatorAndEnded(userId, null);
    }

    List<Poll> getPolls(long userId) {
        return pollRepository.findAllByCreatorOrderByCreatedDesc(userId);
    }

    List<Poll> getPolls(long userId, String streamId) {
        return pollRepository.findAllByCreatorAndStreamIdOrderByCreatedDesc(userId, streamId);
    }

    List<PollVote> getVotes(String pollId) {
        return pollVoteRepository.findAllByPollId(pollId);
    }

    public List<PollResult> getPollResults(String pollId) {
        return mongoTemplate
            .aggregate(newAggregation(
                match(new Criteria("pollId").is(pollId)),
                group("answer").count().as("count"),
                project("count").and("answer").previousOperation(),
                sort(new Sort(Sort.Direction.DESC, "count"))
                ),
                "pollVote", PollResult.class
            )
            .getMappedResults();
    }

    void createVote(PollVote vote) {
        pollVoteRepository.save(vote);
        log.info("Vote added to database: {}", vote.toString());
    }

    void createVotes(List<PollVote> votes) {
        pollVoteRepository.saveAll(votes);
        log.info("Rigged votes added to database");
    }

    boolean hasVoted(long userId, String pollId) {
        return pollVoteRepository.findTopByPollIdAndUserId(pollId, userId) != null;
    }

    void changeVote(long userId, String pollId, String answer) {
        PollVote vote = pollVoteRepository.findTopByPollIdAndUserId(pollId, userId);
        vote.setAnswer(answer);
        pollVoteRepository.save(vote);
    }
}
