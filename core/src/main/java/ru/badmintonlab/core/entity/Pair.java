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

@Entity
@Table(name = "pair")
public class Pair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player1_id", nullable = false)
    private Long player1Id;

    @Column(name = "player2_id", nullable = false)
    private Long player2Id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "discipline")
    private Discipline discipline;

    protected Pair() {
    }

    public Long getId() {
        return id;
    }

    public Long getPlayer1Id() {
        return player1Id;
    }

    public Long getPlayer2Id() {
        return player2Id;
    }

    public Discipline getDiscipline() {
        return discipline;
    }
}
