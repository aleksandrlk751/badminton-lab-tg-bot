package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.badmintonlab.core.domain.MatchSide;

import java.math.BigDecimal;

@Entity
@Table(name = "match_player")
public class MatchPlayer {

    @EmbeddedId
    private MatchPlayerId id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "match_side")
    private MatchSide side;

    @Column(name = "rating_before", precision = 6, scale = 1)
    private BigDecimal ratingBefore;

    @Column(name = "rating_delta", precision = 6, scale = 1)
    private BigDecimal ratingDelta;

    protected MatchPlayer() {
    }

    public MatchPlayerId getId() {
        return id;
    }

    public MatchSide getSide() {
        return side;
    }

    public BigDecimal getRatingDelta() {
        return ratingDelta;
    }
}
