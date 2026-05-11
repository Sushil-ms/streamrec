package com.streamrec.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @Column(nullable = false, updatable = false, length = 64)
    private String id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 150)
    private String artist;

    @Column(nullable = false, length = 100)
    private String genre;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal popularityScore;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Track() {
    }

    public Track(String id, String title, String artist, String genre, BigDecimal popularityScore, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.popularityScore = popularityScore;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getGenre() {
        return genre;
    }

    public BigDecimal getPopularityScore() {
        return popularityScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
