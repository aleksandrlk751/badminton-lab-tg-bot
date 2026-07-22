package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "player_rating")
public class PlayerRating {

    @EmbeddedId
    private PlayerRatingId id;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal rating;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlayerRating() {
    }

    public PlayerRating(PlayerRatingId id, BigDecimal rating) {
        this.id = id;
        this.rating = rating;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public PlayerRatingId getId() {
        return id;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
