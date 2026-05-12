package com.streamrec.service;

import com.streamrec.dto.EventType;
import com.streamrec.dto.UserMusicEvent;
import com.streamrec.entity.UserTrackScore;
import com.streamrec.repository.UserTrackScoreRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RecommendationScoringService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationScoringService.class);

    private final UserTrackScoreRepository userTrackScoreRepository;
    private final RecommendationCacheService recommendationCacheService;

    public RecommendationScoringService(UserTrackScoreRepository userTrackScoreRepository,
                                        RecommendationCacheService recommendationCacheService) {
        this.userTrackScoreRepository = userTrackScoreRepository;
        this.recommendationCacheService = recommendationCacheService;
    }

    @Transactional
    public void processEvent(UserMusicEvent event) {
        validateEvent(event);

        UserTrackScore userTrackScore = userTrackScoreRepository.findByUserIdAndTrackId(event.userId(), event.trackId())
                .orElseGet(() -> UserTrackScore.create(event.userId(), event.trackId()));

        userTrackScore.applyScoreDelta(resolveScoreDelta(event.eventType()));
        incrementEventCounter(userTrackScore, event.eventType());

        UserTrackScore savedUserTrackScore = userTrackScoreRepository.save(userTrackScore);
        List<UserTrackScore> topRecommendations = userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc(event.userId());
        recommendationCacheService.cacheRecommendationsFromScores(event.userId(), topRecommendations);

        log.info(
                "Processed recommendation score update. userId={}, trackId={}, eventType={}, score={}, playCount={}, likeCount={}, saveCount={}, skipCount={}",
                savedUserTrackScore.getUserId(),
                savedUserTrackScore.getTrackId(),
                event.eventType(),
                savedUserTrackScore.getScore(),
                savedUserTrackScore.getPlayCount(),
                savedUserTrackScore.getLikeCount(),
                savedUserTrackScore.getSaveCount(),
                savedUserTrackScore.getSkipCount()
        );
    }

    private void validateEvent(UserMusicEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("User music event must not be null");
        }
        if (!StringUtils.hasText(event.userId()) || !StringUtils.hasText(event.trackId())) {
            throw new IllegalArgumentException("User music event must include userId and trackId");
        }
        if (Objects.isNull(event.eventType())) {
            throw new IllegalArgumentException("User music event must include eventType");
        }
    }

    private BigDecimal resolveScoreDelta(EventType eventType) {
        return switch (eventType) {
            case PLAY -> BigDecimal.valueOf(3);
            case LIKE -> BigDecimal.valueOf(5);
            case SAVE -> BigDecimal.valueOf(7);
            case SKIP -> BigDecimal.valueOf(-4);
        };
    }

    private void incrementEventCounter(UserTrackScore userTrackScore, EventType eventType) {
        switch (eventType) {
            case PLAY -> userTrackScore.incrementPlayCount();
            case LIKE -> userTrackScore.incrementLikeCount();
            case SAVE -> userTrackScore.incrementSaveCount();
            case SKIP -> userTrackScore.incrementSkipCount();
        }
    }
}
