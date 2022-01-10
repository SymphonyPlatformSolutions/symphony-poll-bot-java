package com.symphony.devrel.pollbot.repository;

import com.symphony.devrel.pollbot.model.Poll;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PollRepository extends MongoRepository<Poll, String> {
    long countByCreatorAndEnded(long creator, Instant ended);
    Poll findTopByCreatorAndEnded(long creator, Instant ended);
    List<Poll> findAllByCreatorAndEndedIsNotNullOrderByCreatedDesc(long creator, Pageable pageable);
    List<Poll> findAllByCreatorAndStreamIdAndEndedIsNotNullOrderByCreatedDesc(long creator, String streamId, Pageable pageable);
    Poll findTopByCreatorAndEndedIsNullOrderByCreatedDesc(long creator);
    Poll findTopByCreatorAndStreamIdAndEndedIsNullOrderByCreatedDesc(long creator, String streamId);
}
