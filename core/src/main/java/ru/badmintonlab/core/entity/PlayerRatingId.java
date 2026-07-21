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
public class PlayerRatingId implements Serializable {

    @Column(name = "player_id")
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "discipline")
    private Discipline discipline;

    protected PlayerRatingId() {
    }

    public PlayerRatingId(Long playerId, Discipline discipline) {
        this.playerId = playerId;
        this.discipline = discipline;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerRatingId that)) {
            return false;
        }
        return Objects.equals(playerId, that.playerId) && discipline == that.discipline;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, discipline);
    }
}
