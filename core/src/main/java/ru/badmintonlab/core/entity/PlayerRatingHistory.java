package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.badmintonlab.core.domain.Discipline;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "player_rating_history")
public class PlayerRatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "discipline")
    private Discipline discipline;

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal rating;

    protected PlayerRatingHistory() {
    }

    public PlayerRatingHistory(Long playerId, Discipline discipline, LocalDate recordedAt, BigDecimal rating) {
        this.playerId = playerId;
        this.discipline = discipline;
        this.recordedAt = recordedAt;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public LocalDate getRecordedAt() {
        return recordedAt;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
}
