package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TournamentRegistrationId implements Serializable {

    @Column(name = "tournament_id")
    private Long tournamentId;

    @Column(name = "player_id")
    private Long playerId;

    protected TournamentRegistrationId() {
    }

    public TournamentRegistrationId(Long tournamentId, Long playerId) {
        this.tournamentId = tournamentId;
        this.playerId = playerId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TournamentRegistrationId that)) {
            return false;
        }
        return Objects.equals(tournamentId, that.tournamentId) && Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tournamentId, playerId);
    }
}
