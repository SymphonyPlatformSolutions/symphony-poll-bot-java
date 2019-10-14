package com.symphony.ps.pollbot.repository;

import com.symphony.ps.pollbot.model.Poll;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PollRepository extends MongoRepository<Poll, String> {
    long countByCreatorAndEnded(long creator, Instant ended);
    Poll findTopByCreatorAndEnded(long creator, Instant ended);
    List<Poll> findAllByCreatorOrderByCreatedDesc(long creator, Pageable pageable);
    List<Poll> findAllByCreatorAndStreamIdOrderByCreatedDesc(long creator, String streamId, Pageable pageable);
}
