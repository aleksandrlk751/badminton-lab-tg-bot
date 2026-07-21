package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MatchPlayerId implements Serializable {

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "player_id")
    private Long playerId;

    protected MatchPlayerId() {
    }

    public MatchPlayerId(Long matchId, Long playerId) {
        this.matchId = matchId;
        this.playerId = playerId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MatchPlayerId that)) {
            return false;
        }
        return Objects.equals(matchId, that.matchId) && Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, playerId);
    }
}
