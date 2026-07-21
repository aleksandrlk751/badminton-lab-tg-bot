package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.badmintonlab.core.domain.Discipline;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RivalSummaryId implements Serializable {

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "opponent_id")
    private Long opponentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "discipline")
    private Discipline discipline;

    protected RivalSummaryId() {
    }

    public RivalSummaryId(Long playerId, Long opponentId, Discipline discipline) {
        this.playerId = playerId;
        this.opponentId = opponentId;
        this.discipline = discipline;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getOpponentId() {
        return opponentId;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RivalSummaryId that)) {
            return false;
        }
        return Objects.equals(playerId, that.playerId)
                && Objects.equals(opponentId, that.opponentId)
                && discipline == that.discipline;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, opponentId, discipline);
    }
}
