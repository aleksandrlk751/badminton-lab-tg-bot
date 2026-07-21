package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "snapshot_meta")
public class SnapshotMeta {

    @Id
    @Column(name = "region_code", length = 16)
    private String regionCode;

    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;

    @Column(name = "tournaments_from")
    private LocalDate tournamentsFrom;

    @Column(name = "tournaments_to")
    private LocalDate tournamentsTo;

    protected SnapshotMeta() {
    }

    public SnapshotMeta(String regionCode, Instant lastSyncAt, LocalDate tournamentsFrom, LocalDate tournamentsTo) {
        this.regionCode = regionCode;
        this.lastSyncAt = lastSyncAt;
        this.tournamentsFrom = tournamentsFrom;
        this.tournamentsTo = tournamentsTo;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public LocalDate getTournamentsFrom() {
        return tournamentsFrom;
    }

    public LocalDate getTournamentsTo() {
        return tournamentsTo;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public void setTournamentsFrom(LocalDate tournamentsFrom) {
        this.tournamentsFrom = tournamentsFrom;
    }

    public void setTournamentsTo(LocalDate tournamentsTo) {
        this.tournamentsTo = tournamentsTo;
    }
}
