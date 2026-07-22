package ru.badmintonlab.core.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.RivalSummary;
import ru.badmintonlab.core.entity.RivalSummaryId;
import ru.badmintonlab.core.repository.projection.DisciplineGamesView;
import ru.badmintonlab.core.repository.projection.RivalView;

import java.util.List;

public interface RivalSummaryRepository extends JpaRepository<RivalSummary, RivalSummaryId> {

    /**
     * Топ соперников игрока в дисциплине по числу встреч (для экрана «Соперники» с пагинацией).
     */
    @Query("""
            SELECT p.id AS opponentId, p.nick AS nick, p.lastName AS lastName,
                   p.firstName AS firstName, p.city AS city,
                   rs.wins AS wins, rs.losses AS losses
            FROM RivalSummary rs, Player p
            WHERE p.id = rs.id.opponentId
              AND rs.id.playerId = :playerId
              AND rs.id.discipline = :discipline
            ORDER BY (rs.wins + rs.losses) DESC, rs.wins DESC, p.nick ASC
            """)
    List<RivalView> findTopRivals(@Param("playerId") Long playerId,
                                  @Param("discipline") Discipline discipline,
                                  Pageable pageable);

    long countByIdPlayerIdAndIdDiscipline(Long playerId, Discipline discipline);

    @Query("""
            SELECT COUNT(DISTINCT rs.id.opponentId)
            FROM RivalSummary rs
            WHERE rs.id.playerId = :playerId
            """)
    long countDistinctOpponents(@Param("playerId") Long playerId);

    /**
     * Топ соперников по сумме W–L во всех разрядах (фильтр «Все»).
     */
    @Query("""
            SELECT p.id AS opponentId, p.nick AS nick, p.lastName AS lastName,
                   p.firstName AS firstName, p.city AS city,
                   SUM(rs.wins) AS wins, SUM(rs.losses) AS losses
            FROM RivalSummary rs, Player p
            WHERE p.id = rs.id.opponentId
              AND rs.id.playerId = :playerId
            GROUP BY p.id, p.nick, p.lastName, p.firstName, p.city
            ORDER BY SUM(rs.wins + rs.losses) DESC, SUM(rs.wins) DESC, p.nick ASC
            """)
    List<RivalView> findTopRivalsAllDisciplines(@Param("playerId") Long playerId, Pageable pageable);

    /**
     * Число встреч по каждой дисциплине игрока — для выбора дисциплины по умолчанию
     * и переключателя дисциплин на экране соперников.
     */
    @Query("""
            SELECT rs.id.discipline AS discipline, SUM(rs.wins + rs.losses) AS games
            FROM RivalSummary rs
            WHERE rs.id.playerId = :playerId
            GROUP BY rs.id.discipline
            ORDER BY SUM(rs.wins + rs.losses) DESC
            """)
    List<DisciplineGamesView> disciplineGames(@Param("playerId") Long playerId);
}
