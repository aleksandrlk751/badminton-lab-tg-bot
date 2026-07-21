package ru.badmintonlab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.badmintonlab.core.domain.TournamentStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tournament")
public class Tournament {

    @Id
    private Long id;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(name = "category_code", length = 16)
    private String categoryCode;

    @Column(name = "rating_limit", precision = 6, scale = 1)
    private BigDecimal ratingLimit;

    @Column(name = "avg_rating", precision = 6, scale = 1)
    private BigDecimal avgRating;

    @Column(precision = 4, scale = 2)
    private BigDecimal coefficient;

    @Column(name = "hall_id")
    private Long hallId;

    @Column(length = 128)
    private String city;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "tournament_status")
    private TournamentStatus status;

    @Column(name = "region_code", nullable = false, length = 16)
    private String regionCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tournament() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public String getRegionCode() {
        return regionCode;
    }
}
