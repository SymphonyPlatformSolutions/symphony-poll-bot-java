package com.symphony.devrel.pollbot.service;

import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.StreamType.TypeEnum;
import com.symphony.devrel.pollbot.model.*;
import com.symphony.devrel.pollbot.repository.PollRepository;
import com.symphony.devrel.pollbot.repository.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {
    private final MongoTemplate mongoTemplate;
    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final StreamService streamService;

    @Cacheable("im-stream-id")
    public String getImStreamId(long userId) {
        return streamService.create(userId).getId();
    }

    @Cacheable("stream-type")
    public TypeEnum getStreamType(String streamId) {
        return TypeEnum.fromValue(streamService.getStream(streamId).getStreamType().getType());
    }

    @Cacheable("room-read-only")
    public boolean isRoomReadOnly(String streamId) {
        return Boolean.TRUE.equals(streamService.getRoomInfo(streamId).getRoomAttributes().getReadOnly());
    }

    public boolean hasActivePoll(long userId) {
        return 1L == pollRepository.countByCreatorAndEnded(userId, null);
    }

    public void savePoll(Poll poll) {
        pollRepository.save(poll);
        log.info("Poll saved to database: {}", poll);
    }

    public void endPoll(long userId) {
        Poll poll = getActivePoll(userId);
        poll.setEnded(Instant.now());
        pollRepository.save(poll);
    }

    public Poll getPoll(String id) {
        return pollRepository.findById(id).orElse(null);
    }

    public Poll getActivePoll(long userId) {
        return pollRepository.findTopByCreatorAndEnded(userId, null);
    }

    private List<Poll> getHistoricalPolls(long userId, String streamId, int count) {
        PageRequest page = PageRequest.of(0, count);
        List<Poll> polls = (streamId != null) ?
            pollRepository.findAllByCreatorAndStreamIdAndEndedIsNotNullOrderByCreatedDesc(userId, streamId, page) :
            pollRepository.findAllByCreatorAndEndedIsNotNullOrderByCreatedDesc(userId, page);
        polls.sort(Comparator.comparing(Poll::getCreated));
        return polls;
    }

    private List<Poll> getActivePoll(long userId, String streamId) {
        Poll poll = (streamId != null) ?
            pollRepository.findTopByCreatorAndStreamIdAndEndedIsNullOrderByCreatedDesc(userId, streamId) :
            pollRepository.findTopByCreatorAndEndedIsNullOrderByCreatedDesc(userId);
        return poll == null ? List.of() : List.of(poll);
    }

    public List<PollVote> getVotes(String pollId) {
        return pollVoteRepository.findAllByPollId(pollId);
    }

    public List<PollResult> getPollResults(String pollId) {
        return mongoTemplate
            .aggregate(newAggregation(
                match(new Criteria("pollId").is(pollId)),
                group("answer").count().as("count"),
                project("count").and("answer").previousOperation(),
                sort(Sort.by(new Sort.Order(Sort.Direction.DESC, "count")))
            ), "pollVote", PollResult.class)
            .getMappedResults();
    }

    public PollHistory getPollHistory(long userId, String streamId, String displayName, int count, boolean isActive) {
        List<Poll> polls = isActive ? getActivePoll(userId, streamId) : getHistoricalPolls(userId, streamId, count);

        if (polls.isEmpty()) {
            return null;
        }

        List<String> pollIds = polls.parallelStream().map(Poll::getId).toList();

        List<PollResult> results = mongoTemplate
            .aggregate(newAggregation(
                match(new Criteria("pollId").in(pollIds)),
                group("pollId", "answer")
                    .count().as("count")
                    .first("pollId").as("pollId")
                    .first("answer").as("answer"),
                project("pollId", "answer", "count"),
                sort(Sort.by(new Sort.Order(Sort.Direction.DESC, "count")))
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
            .room(streamId != null)
            .creatorDisplayName(displayName)
            .polls(pollHistoryItems)
            .build();
    }

    public void createVote(PollVote vote) {
        pollVoteRepository.save(vote);
        log.info("Vote added to database: {}", vote.toString());
    }

    public boolean hasVoted(long userId, String pollId) {
        return pollVoteRepository.findTopByPollIdAndUserId(pollId, userId) != null;
    }

    public void changeVote(long userId, String pollId, String answer) {
        PollVote vote = pollVoteRepository.findTopByPollIdAndUserId(pollId, userId);
        vote.setAnswer(answer);
        pollVoteRepository.save(vote);
    }
}
