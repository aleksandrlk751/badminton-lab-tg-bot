package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tournament_registration")
public class TournamentRegistration {

    @EmbeddedId
    private TournamentRegistrationId id;

    @Column(name = "pair_id")
    private Long pairId;

    @Column(name = "registered_at")
    private Instant registeredAt;

    protected TournamentRegistration() {
    }

    public static TournamentRegistration create(long tournamentId, long playerId, Long pairId, Instant registeredAt) {
        TournamentRegistration reg = new TournamentRegistration();
        reg.setId(new TournamentRegistrationId(tournamentId, playerId));
        reg.setPairId(pairId);
        reg.setRegisteredAt(registeredAt);
        return reg;
    }

    public TournamentRegistrationId getId() {
        return id;
    }

    public Long getPairId() {
        return pairId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setId(TournamentRegistrationId id) {
        this.id = id;
    }

    public void setPairId(Long pairId) {
        this.pairId = pairId;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
