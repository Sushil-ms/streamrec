package com.streamrec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.streamrec.dto.RecommendationItem;
import com.streamrec.entity.Track;
import com.streamrec.entity.UserTrackScore;
import com.streamrec.repository.TrackRepository;
import com.streamrec.repository.UserTrackScoreRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationCacheService recommendationCacheService;

    @Mock
    private UserTrackScoreRepository userTrackScoreRepository;

    @Mock
    private TrackRepository trackRepository;

    @Captor
    private ArgumentCaptor<List<RecommendationItem>> recommendationItemsCaptor;

    @Test
    void getRecommendationsReturnsFromRedisWhenCacheHit() {
        Instant generatedAt = Instant.parse("2026-05-12T10:00:00Z");
        RecommendationItem cachedItem = new RecommendationItem(
                "track_456",
                "Midnight Drive",
                "Nova Lane",
                "Synthwave",
                BigDecimal.valueOf(0.91),
                "Because you recently played similar Synthwave tracks"
        );
        when(recommendationCacheService.getRecommendations("user_123"))
                .thenReturn(Optional.of(new RecommendationCacheService.CachedRecommendations(generatedAt, List.of(cachedItem))));

        RecommendationService service = new RecommendationService(
                recommendationCacheService,
                userTrackScoreRepository,
                trackRepository
        );

        var response = service.getRecommendations("user_123");

        assertThat(response.userId()).isEqualTo("user_123");
        assertThat(response.generatedAt()).isEqualTo(generatedAt);
        assertThat(response.recommendations()).containsExactly(cachedItem);
        assertThat(response.metadata().source()).isEqualTo("redis");
        assertThat(response.metadata().modelVersion()).isEqualTo("v1");
        assertThat(response.metadata().latencyMs()).isGreaterThanOrEqualTo(0L);
        verify(userTrackScoreRepository, never()).findTop10ByUserIdOrderByScoreDesc("user_123");
    }

    @Test
    void getRecommendationsFallsBackToPostgresWhenRedisMiss() {
        UserTrackScore score = new UserTrackScore(
                "score_123",
                "user_123",
                "track_456",
                BigDecimal.valueOf(12.50),
                3L,
                1L,
                0L,
                0L,
                Instant.parse("2026-05-12T09:30:00Z"),
                Instant.parse("2026-05-12T09:00:00Z")
        );
        Track track = new Track(
                "track_456",
                "Midnight Drive",
                "Nova Lane",
                "Synthwave",
                BigDecimal.valueOf(0.80),
                Instant.parse("2026-05-10T08:00:00Z")
        );

        when(recommendationCacheService.getRecommendations("user_123")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of(score));
        when(trackRepository.findAllById(List.of("track_456"))).thenReturn(List.of(track));

        RecommendationService service = new RecommendationService(
                recommendationCacheService,
                userTrackScoreRepository,
                trackRepository
        );

        var response = service.getRecommendations("user_123");

        assertThat(response.recommendations()).hasSize(1);
        RecommendationItem item = response.recommendations().getFirst();
        assertThat(item.trackId()).isEqualTo("track_456");
        assertThat(item.title()).isEqualTo("Midnight Drive");
        assertThat(item.artist()).isEqualTo("Nova Lane");
        assertThat(item.genre()).isEqualTo("Synthwave");
        assertThat(item.score()).isEqualByComparingTo("12.50");
        assertThat(item.reason()).isEqualTo("Because you liked this track");
        assertThat(response.metadata().source()).isEqualTo("postgres_fallback");

        verify(recommendationCacheService).cacheRecommendations(
                org.mockito.ArgumentMatchers.eq("user_123"),
                recommendationItemsCaptor.capture()
        );
        assertThat(recommendationItemsCaptor.getValue()).containsExactly(item);
    }

    @Test
    void getRecommendationsReturnsEmptyListWhenNoDataExists() {
        when(recommendationCacheService.getRecommendations("user_123")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of());

        RecommendationService service = new RecommendationService(
                recommendationCacheService,
                userTrackScoreRepository,
                trackRepository
        );

        var response = service.getRecommendations("user_123");

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.metadata().source()).isEqualTo("empty");
        verify(recommendationCacheService, never()).cacheRecommendations(org.mockito.ArgumentMatchers.eq("user_123"), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getRecommendationsUsesUnknownMetadataWhenTrackIsMissing() {
        UserTrackScore score = new UserTrackScore(
                "score_123",
                "user_123",
                "track_missing",
                BigDecimal.valueOf(-4),
                0L,
                0L,
                0L,
                1L,
                Instant.parse("2026-05-12T09:30:00Z"),
                Instant.parse("2026-05-12T09:00:00Z")
        );

        when(recommendationCacheService.getRecommendations("user_123")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of(score));
        when(trackRepository.findAllById(List.of("track_missing"))).thenReturn(List.of());

        RecommendationService service = new RecommendationService(
                recommendationCacheService,
                userTrackScoreRepository,
                trackRepository
        );

        var response = service.getRecommendations("user_123");

        RecommendationItem item = response.recommendations().getFirst();
        assertThat(item.title()).isEqualTo("Unknown Track");
        assertThat(item.artist()).isEqualTo("Unknown Artist");
        assertThat(item.genre()).isEqualTo("Unknown Genre");
        assertThat(item.score()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.reason()).isEqualTo("Lower-ranked due to skip activity");
    }
}
