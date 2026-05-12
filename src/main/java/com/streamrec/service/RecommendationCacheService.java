package com.streamrec.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamrec.dto.RecommendationItem;
import com.streamrec.entity.UserTrackScore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationCacheService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationCacheService.class);
    private static final String RECOMMENDATION_KEY_PREFIX = "rec:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration recommendationTtl;

    public RecommendationCacheService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${streamrec.cache.recommendation-ttl:PT1H}") Duration recommendationTtl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.recommendationTtl = recommendationTtl;
    }

    public Optional<CachedRecommendations> getRecommendations(String userId) {
        String serializedPayload = stringRedisTemplate.opsForValue().get(buildRecommendationKey(userId));
        if (serializedPayload == null) {
            return Optional.empty();
        }

        try {
            RecommendationCachePayload payload = objectMapper.readValue(serializedPayload, RecommendationCachePayload.class);
            List<RecommendationItem> items = payload.recommendations().stream()
                    .map(item -> new RecommendationItem(
                            item.trackId(),
                            item.title(),
                            item.artist(),
                            item.genre(),
                            item.score(),
                            item.reason()
                    ))
                    .toList();
            return Optional.of(new CachedRecommendations(payload.generatedAt(), items));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize recommendation cache payload for userId=" + userId, exception);
        }
    }

    public void cacheRecommendations(String userId, List<RecommendationItem> items) {
        RecommendationCachePayload payload = new RecommendationCachePayload(userId, Instant.now(), items.stream()
                .map(item -> new RecommendationCacheItem(
                        item.trackId(),
                        item.title(),
                        item.artist(),
                        item.genre(),
                        item.score(),
                        item.reason()
                ))
                .toList());

        writeRecommendations(userId, payload);
    }

    public void cacheRecommendationsFromScores(String userId, List<UserTrackScore> userTrackScores) {
        RecommendationCachePayload payload = new RecommendationCachePayload(
                userId,
                Instant.now(),
                userTrackScores.stream()
                        .map(this::toCacheItem)
                        .toList()
        );

        writeRecommendations(userId, payload);
    }

    private void writeRecommendations(String userId, RecommendationCachePayload payload) {
        try {
            String key = buildRecommendationKey(userId);
            String serializedPayload = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.opsForValue().set(key, serializedPayload, recommendationTtl);
            log.info("Refreshed recommendation cache. userId={}, key={}, itemCount={}", userId, key, payload.recommendations().size());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize recommendation cache payload for userId=" + userId, exception);
        }
    }

    String buildRecommendationKey(String userId) {
        return RECOMMENDATION_KEY_PREFIX + userId;
    }

    private RecommendationCacheItem toCacheItem(UserTrackScore userTrackScore) {
        return new RecommendationCacheItem(
                userTrackScore.getTrackId(),
                "Unknown Track",
                "Unknown Artist",
                "Unknown Genre",
                userTrackScore.getScore().max(BigDecimal.ZERO),
                buildReason(userTrackScore)
        );
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

    private record RecommendationCachePayload(
            String userId,
            Instant generatedAt,
            List<RecommendationCacheItem> recommendations
    ) {
    }

    public record CachedRecommendations(
            Instant generatedAt,
            List<RecommendationItem> recommendations
    ) {
    }

    private record RecommendationCacheItem(
            String trackId,
            String title,
            String artist,
            String genre,
            BigDecimal score,
            String reason
    ) {
    }
}
