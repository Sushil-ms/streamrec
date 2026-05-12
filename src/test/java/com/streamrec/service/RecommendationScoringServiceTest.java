package com.streamrec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.streamrec.dto.EventType;
import com.streamrec.dto.UserMusicEvent;
import com.streamrec.entity.UserTrackScore;
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
class RecommendationScoringServiceTest {

    @Mock
    private UserTrackScoreRepository userTrackScoreRepository;

    @Mock
    private RecommendationCacheService recommendationCacheService;

    @Captor
    private ArgumentCaptor<UserTrackScore> userTrackScoreCaptor;

    @Test
    void processEventPlayIncreasesScoreByThreeAndIncrementsPlayCount() {
        UserMusicEvent event = new UserMusicEvent("user_123", "track_456", EventType.PLAY, Instant.parse("2026-05-12T10:00:00Z"));

        when(userTrackScoreRepository.findByUserIdAndTrackId("user_123", "track_456")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.save(any(UserTrackScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of());

        RecommendationScoringService service = new RecommendationScoringService(userTrackScoreRepository, recommendationCacheService);

        service.processEvent(event);

        verify(userTrackScoreRepository).save(userTrackScoreCaptor.capture());
        UserTrackScore savedScore = userTrackScoreCaptor.getValue();
        assertThat(savedScore.getUserId()).isEqualTo("user_123");
        assertThat(savedScore.getTrackId()).isEqualTo("track_456");
        assertThat(savedScore.getScore()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(savedScore.getPlayCount()).isEqualTo(1);
        assertThat(savedScore.getLikeCount()).isZero();
        assertThat(savedScore.getSaveCount()).isZero();
        assertThat(savedScore.getSkipCount()).isZero();
        verify(recommendationCacheService).cacheRecommendationsFromScores("user_123", List.of());
    }

    @Test
    void processEventLikeIncreasesScoreByFiveAndIncrementsLikeCount() {
        UserMusicEvent event = new UserMusicEvent("user_123", "track_456", EventType.LIKE, Instant.parse("2026-05-12T10:00:00Z"));

        when(userTrackScoreRepository.findByUserIdAndTrackId("user_123", "track_456")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.save(any(UserTrackScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of());

        RecommendationScoringService service = new RecommendationScoringService(userTrackScoreRepository, recommendationCacheService);

        service.processEvent(event);

        verify(userTrackScoreRepository).save(userTrackScoreCaptor.capture());
        UserTrackScore savedScore = userTrackScoreCaptor.getValue();
        assertThat(savedScore.getScore()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(savedScore.getPlayCount()).isZero();
        assertThat(savedScore.getLikeCount()).isEqualTo(1);
        assertThat(savedScore.getSaveCount()).isZero();
        assertThat(savedScore.getSkipCount()).isZero();
    }

    @Test
    void processEventSaveIncreasesScoreBySevenAndIncrementsSaveCount() {
        UserMusicEvent event = new UserMusicEvent("user_123", "track_456", EventType.SAVE, Instant.parse("2026-05-12T10:00:00Z"));

        when(userTrackScoreRepository.findByUserIdAndTrackId("user_123", "track_456")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.save(any(UserTrackScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of());

        RecommendationScoringService service = new RecommendationScoringService(userTrackScoreRepository, recommendationCacheService);

        service.processEvent(event);

        verify(userTrackScoreRepository).save(userTrackScoreCaptor.capture());
        UserTrackScore savedScore = userTrackScoreCaptor.getValue();
        assertThat(savedScore.getScore()).isEqualByComparingTo(BigDecimal.valueOf(7));
        assertThat(savedScore.getPlayCount()).isZero();
        assertThat(savedScore.getLikeCount()).isZero();
        assertThat(savedScore.getSaveCount()).isEqualTo(1);
        assertThat(savedScore.getSkipCount()).isZero();
    }

    @Test
    void processEventSkipDecreasesScoreByFourAndIncrementsSkipCount() {
        UserMusicEvent event = new UserMusicEvent("user_123", "track_456", EventType.SKIP, Instant.parse("2026-05-12T10:00:00Z"));

        when(userTrackScoreRepository.findByUserIdAndTrackId("user_123", "track_456")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.save(any(UserTrackScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of());

        RecommendationScoringService service = new RecommendationScoringService(userTrackScoreRepository, recommendationCacheService);

        service.processEvent(event);

        verify(userTrackScoreRepository).save(userTrackScoreCaptor.capture());
        UserTrackScore savedScore = userTrackScoreCaptor.getValue();
        assertThat(savedScore.getScore()).isEqualByComparingTo(BigDecimal.valueOf(-4));
        assertThat(savedScore.getPlayCount()).isZero();
        assertThat(savedScore.getLikeCount()).isZero();
        assertThat(savedScore.getSaveCount()).isZero();
        assertThat(savedScore.getSkipCount()).isEqualTo(1);
    }

    @Test
    void processEventCreatesNewAggregateWhenMissing() {
        UserMusicEvent event = new UserMusicEvent("user_123", "track_456", EventType.PLAY, Instant.parse("2026-05-12T10:00:00Z"));

        when(userTrackScoreRepository.findByUserIdAndTrackId("user_123", "track_456")).thenReturn(Optional.empty());
        when(userTrackScoreRepository.save(any(UserTrackScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(List.of());

        RecommendationScoringService service = new RecommendationScoringService(userTrackScoreRepository, recommendationCacheService);

        service.processEvent(event);

        verify(userTrackScoreRepository).save(userTrackScoreCaptor.capture());
        UserTrackScore savedScore = userTrackScoreCaptor.getValue();
        assertThat(savedScore.getId()).isNull();
        assertThat(savedScore.getUserId()).isEqualTo("user_123");
        assertThat(savedScore.getTrackId()).isEqualTo("track_456");
    }

    @Test
    void processEventUpdatesExistingAggregateWhenPresent() {
        UserTrackScore existingScore = new UserTrackScore(
                "score_123",
                "user_123",
                "track_456",
                BigDecimal.TEN,
                2L,
                1L,
                0L,
                0L,
                Instant.parse("2026-05-11T10:00:00Z"),
                Instant.parse("2026-05-10T10:00:00Z")
        );
        List<UserTrackScore> topRecommendations = List.of(existingScore);
        UserMusicEvent event = new UserMusicEvent("user_123", "track_456", EventType.SAVE, Instant.parse("2026-05-12T10:00:00Z"));

        when(userTrackScoreRepository.findByUserIdAndTrackId("user_123", "track_456")).thenReturn(Optional.of(existingScore));
        when(userTrackScoreRepository.save(existingScore)).thenReturn(existingScore);
        when(userTrackScoreRepository.findTop10ByUserIdOrderByScoreDesc("user_123")).thenReturn(topRecommendations);

        RecommendationScoringService service = new RecommendationScoringService(userTrackScoreRepository, recommendationCacheService);

        service.processEvent(event);

        assertThat(existingScore.getScore()).isEqualByComparingTo(BigDecimal.valueOf(17));
        assertThat(existingScore.getPlayCount()).isEqualTo(2);
        assertThat(existingScore.getLikeCount()).isEqualTo(1);
        assertThat(existingScore.getSaveCount()).isEqualTo(1);
        assertThat(existingScore.getSkipCount()).isZero();
        verify(userTrackScoreRepository).save(existingScore);
        verify(recommendationCacheService).cacheRecommendationsFromScores("user_123", topRecommendations);
    }
}
