package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rival_summary")
public class RivalSummary {

    @EmbeddedId
    private RivalSummaryId id;

    @Column(nullable = false)
    private Short wins;

    @Column(nullable = false)
    private Short losses;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RivalSummary() {
    }

    public RivalSummaryId getId() {
        return id;
    }

    public Short getWins() {
        return wins;
    }

    public Short getLosses() {
        return losses;
    }
}
