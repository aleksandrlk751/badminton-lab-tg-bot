package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "participation")
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "pair_id")
    private Long pairId;

    private Short place;

    @Column(name = "rating_before", precision = 6, scale = 1)
    private BigDecimal ratingBefore;

    @Column(name = "rating_delta", precision = 6, scale = 1)
    private BigDecimal ratingDelta;

    @Column(name = "rating_after", precision = 6, scale = 1)
    private BigDecimal ratingAfter;

    @Column(name = "matches_won")
    private Short matchesWon;

    @Column(name = "matches_lost")
    private Short matchesLost;

    @Column(name = "sets_won")
    private Short setsWon;

    @Column(name = "sets_lost")
    private Short setsLost;

    protected Participation() {
    }

    public Long getId() {
        return id;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getPairId() {
        return pairId;
    }
}
