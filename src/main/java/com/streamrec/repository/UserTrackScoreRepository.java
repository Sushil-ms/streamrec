package com.streamrec.repository;

import com.streamrec.entity.UserTrackScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTrackScoreRepository extends JpaRepository<UserTrackScore, String> {

    Optional<UserTrackScore> findByUserIdAndTrackId(String userId, String trackId);

    List<UserTrackScore> findTop10ByUserIdOrderByScoreDesc(String userId);
}
