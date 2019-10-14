package com.symphony.ps.pollbot.services;

import com.symphony.ps.pollbot.model.*;
import com.symphony.ps.pollbot.repository.PollRepository;
import com.symphony.ps.pollbot.repository.PollVoteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    private List<Poll> getLastTenPolls(long userId) {
        List<Poll> polls = pollRepository
            .findAllByCreatorOrderByCreatedDesc(userId, PageRequest.of(0, 10));
        polls.sort(Comparator.comparing(Poll::getCreated));
        return polls;
    }

    private List<Poll> getLastTenPolls(long userId, String streamId) {
        List<Poll> polls = pollRepository
            .findAllByCreatorAndStreamIdOrderByCreatedDesc(userId, streamId, PageRequest.of(0, 10));
        polls.sort(Comparator.comparing(Poll::getCreated));
        return polls;
    }

    List<PollVote> getVotes(String pollId) {
        return pollVoteRepository.findAllByPollId(pollId);
    }

    List<PollResult> getPollResults(String pollId) {
        return mongoTemplate
            .aggregate(newAggregation(
                match(new Criteria("pollId").is(pollId)),
                group("answer").count().as("count"),
                project("count").and("answer").previousOperation(),
                sort(new Sort(Sort.Direction.DESC, "count"))
            ), "pollVote", PollResult.class)
            .getMappedResults();
    }

    PollHistory getPollHistory(long userId, String streamId) {
        boolean isRoom = streamId != null;
        List<Poll> polls = isRoom ? getLastTenPolls(userId, streamId) : getLastTenPolls(userId);

        if (polls.isEmpty()) {
            return null;
        }

        List<String> pollIds = polls.parallelStream()
            .map(Poll::getId)
            .collect(Collectors.toList());

        List<PollResult> results = mongoTemplate
            .aggregate(newAggregation(
                match(new Criteria("pollId").in(pollIds)),
                group("pollId", "answer")
                    .count().as("count")
                    .first("pollId").as("pollId")
                    .first("answer").as("answer"),
                project("pollId", "answer", "count"),
                sort(new Sort(Sort.Direction.DESC, "count"))
            ), "pollVote", PollResult.class)
            .getMappedResults();

        List<PollHistoryItem> pollHistoryItems = new ArrayList<>();
        polls.forEach(poll -> {
            List<PollResult> thisPollResults = results.parallelStream()
                .filter(r -> r.getPollId().equals(poll.getId()))
                .collect(Collectors.toList());

            // Add in widths
            if (!thisPollResults.isEmpty()) {
                long maxVal = Collections.max(thisPollResults, Comparator.comparingLong(PollResult::getCount)).getCount();
                thisPollResults.forEach(r -> r.setWidth(Math.max(1, (int) (((float) r.getCount() / maxVal) * 200))));
            }

            // Add in 0 votes for options nobody voted on
            poll.getAnswers().stream()
                .map(PollResult::new)
                .filter(a -> !thisPollResults.contains(a))
                .forEach(thisPollResults::add);

            pollHistoryItems.add(PollHistoryItem.builder()
                .created(poll.getCreated())
                .ended(poll.getEnded())
                .questionText(poll.getQuestionText())
                .results(thisPollResults)
                .build()
            );
        });

        return PollHistory.builder()
            .room(isRoom)
            .creatorId(userId)
            .polls(pollHistoryItems)
            .build();
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
