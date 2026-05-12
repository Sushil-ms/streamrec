package com.streamrec.service;

import com.streamrec.dto.RecommendationItem;
import com.streamrec.dto.RecommendationMetadata;
import com.streamrec.dto.RecommendationResponse;
import com.streamrec.entity.Track;
import com.streamrec.entity.UserTrackScore;
import com.streamrec.repository.TrackRepository;
import com.streamrec.repository.UserTrackScoreRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private static final String REDIS_SOURCE = "redis";
    private static final String POSTGRES_FALLBACK_SOURCE = "postgres_fallback";
    private static final String EMPTY_SOURCE = "empty";
    private static final String MODEL_VERSION = "v1";
    private static final String UNKNOWN_TRACK = "Unknown Track";
    private static final String UNKNOWN_ARTIST = "Unknown Artist";
    private static final String UNKNOWN_GENRE = "Unknown Genre";

    private final RecommendationCacheService recommendationCacheService;
    private final UserTrackScoreRepository userTrackScoreRepository;
    private final TrackRepository trackRepository;

    public RecommendationService(RecommendationCacheService recommendationCacheService,
                                 UserTrackScoreRepository userTrackScoreRepository,
                                 TrackRepository trackRepository) {
        this.recommendationCacheService = recommendationCacheService;
        this.userTrackScoreRepository = userTrackScoreRepository;
        this.trackRepository = trackRepository;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(String userId) {
        long startedAt = System.currentTimeMillis();

        return recommendationCacheService.getRecommendations(userId)
                .map(cachedRecommendations -> buildResponse(
                        userId,
                        cachedRecommendations.generatedAt(),
                        cachedRecommendations.recommendations(),
                        REDIS_SOURCE,
                        startedAt
                ))
                .orElseGet(() -> loadFromPostgres(userId, startedAt));
    }

    private RecommendationResponse loadFromPostgres(String userId, long startedAt) {
        List<UserTrackScore> userTrackScores = userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc(userId);
        if (userTrackScores.isEmpty()) {
            return buildResponse(userId, Instant.now(), List.of(), EMPTY_SOURCE, startedAt);
        }

        List<RecommendationItem> recommendations = buildRecommendations(userTrackScores);
        recommendationCacheService.cacheRecommendations(userId, recommendations);
        return buildResponse(userId, Instant.now(), recommendations, POSTGRES_FALLBACK_SOURCE, startedAt);
    }

    private List<RecommendationItem> buildRecommendations(List<UserTrackScore> userTrackScores) {
        Map<String, Track> tracksById = new LinkedHashMap<>();
        trackRepository.findAllById(userTrackScores.stream().map(UserTrackScore::getTrackId).toList())
                .forEach(track -> tracksById.put(track.getId(), track));

        return userTrackScores.stream()
                .map(userTrackScore -> toRecommendationItem(userTrackScore, tracksById.get(userTrackScore.getTrackId())))
                .toList();
    }

    private RecommendationItem toRecommendationItem(UserTrackScore userTrackScore, Track track) {
        return new RecommendationItem(
                userTrackScore.getTrackId(),
                track != null ? track.getTitle() : UNKNOWN_TRACK,
                track != null ? track.getArtist() : UNKNOWN_ARTIST,
                track != null ? track.getGenre() : UNKNOWN_GENRE,
                normalizeScore(userTrackScore.getScore()),
                buildReason(userTrackScore)
        );
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        return score.max(BigDecimal.ZERO);
    }

    private String buildReason(UserTrackScore userTrackScore) {
        if (userTrackScore.getSaveCount() > 0) {
            return "Because you saved this track";
        }
        if (userTrackScore.getLikeCount() > 0) {
            return "Because you liked this track";
        }
        if (userTrackScore.getPlayCount() > 0) {
            return "Because you played this track recently";
        }
        if (userTrackScore.getSkipCount() > 0) {
            return "Lower-ranked due to skip activity";
        }
        return "Based on your listening activity";
    }

    private RecommendationResponse buildResponse(String userId,
                                                 Instant generatedAt,
                                                 List<RecommendationItem> recommendations,
                                                 String source,
                                                 long startedAt) {
        RecommendationMetadata metadata = new RecommendationMetadata(
                source,
                MODEL_VERSION,
                Math.max(0L, System.currentTimeMillis() - startedAt)
        );

        return new RecommendationResponse(userId, generatedAt, recommendations, metadata);
    }
}
