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

    public TournamentRegistrationId getId() {
        return id;
    }

    public Long getPairId() {
        return pairId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
