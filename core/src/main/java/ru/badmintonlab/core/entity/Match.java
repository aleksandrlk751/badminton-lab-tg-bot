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

import java.time.Instant;

@Entity
@Table(name = "match")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "discipline")
    private Discipline discipline;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(length = 64)
    private String stage;

    @Column(name = "score_sets", nullable = false, length = 16)
    private String scoreSets;

    @Column(name = "duration_min")
    private Short durationMin;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "external_key", length = 128)
    private String externalKey;

    protected Match() {
    }

    public Match(Long tournamentId, Discipline discipline, Instant playedAt, String scoreSets, String source) {
        this.tournamentId = tournamentId;
        this.discipline = discipline;
        this.playedAt = playedAt;
        this.scoreSets = scoreSets;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getScoreSets() {
        return scoreSets;
    }

    public Short getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Short durationMin) {
        this.durationMin = durationMin;
    }

    public String getSource() {
        return source;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }
}
