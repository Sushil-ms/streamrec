package com.streamrec.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_track_scores",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_track_score_user_track", columnNames = {"user_id", "track_id"})
)
public class UserTrackScore {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "track_id", nullable = false, length = 64)
    private String trackId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private long playCount;

    @Column(nullable = false)
    private long likeCount;

    @Column(nullable = false)
    private long saveCount;

    @Column(nullable = false)
    private long skipCount;

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserTrackScore() {
    }

    public UserTrackScore(String id, String userId, String trackId, BigDecimal score,
                          long playCount, long likeCount, long saveCount, long skipCount,
                          Instant lastUpdatedAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.trackId = trackId;
        this.score = score;
        this.playCount = playCount;
        this.likeCount = likeCount;
        this.saveCount = saveCount;
        this.skipCount = skipCount;
        this.lastUpdatedAt = lastUpdatedAt;
        this.createdAt = createdAt;
    }

    public static UserTrackScore create(String userId, String trackId) {
        return new UserTrackScore(
                null,
                userId,
                trackId,
                BigDecimal.ZERO,
                0L,
                0L,
                0L,
                0L,
                null,
                null
        );
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        lastUpdatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        lastUpdatedAt = Instant.now();
    }

    public void applyScoreDelta(BigDecimal delta) {
        score = score.add(delta);
    }

    public void incrementPlayCount() {
        playCount++;
    }

    public void incrementLikeCount() {
        likeCount++;
    }

    public void incrementSaveCount() {
        saveCount++;
    }

    public void incrementSkipCount() {
        skipCount++;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTrackId() {
        return trackId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public long getPlayCount() {
        return playCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getSaveCount() {
        return saveCount;
    }

    public long getSkipCount() {
        return skipCount;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
