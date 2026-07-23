package ru.badmintonlab.core.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Participation;
import ru.badmintonlab.core.repository.projection.LastTournamentView;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    /**
     * Последние турниры игрока по дате старта (для карточки берём первый).
     */
    @Query("""
            SELECT t.id AS tournamentId, t.name AS name, t.startsAt AS startsAt, pa.place AS place
            FROM Participation pa, Tournament t
            WHERE t.id = pa.tournamentId AND pa.playerId = :playerId
            ORDER BY t.startsAt DESC
            """)
    List<LastTournamentView> findLastTournaments(@Param("playerId") Long playerId, Pageable pageable);

    /** Турниры, где участвовали оба игрока — для lazy-fetch gamesd. */
    @Query("""
            SELECT pa1.tournamentId
            FROM Participation pa1, Participation pa2
            WHERE pa1.playerId = :playerA
              AND pa2.playerId = :playerB
              AND pa1.tournamentId = pa2.tournamentId
            ORDER BY pa1.tournamentId DESC
            """)
    List<Long> findCommonTournamentIds(@Param("playerA") long playerA, @Param("playerB") long playerB);

    /** Дисциплины пар из участий игрока (MD/WD и т.д.) — для fallback пола. */
    @Query("""
            SELECT DISTINCT pr.discipline
            FROM Participation pa
            JOIN Pair pr ON pr.id = pa.pairId
            WHERE pa.playerId = :playerId
            """)
    List<Discipline> findPairDisciplinesByPlayerId(@Param("playerId") long playerId);

    /** Коды категорий турниров, где игрок участвовал — fallback, если в паре generic D. */
    @Query("""
            SELECT DISTINCT t.categoryCode
            FROM Participation pa
            JOIN Tournament t ON t.id = pa.tournamentId
            WHERE pa.playerId = :playerId AND t.categoryCode IS NOT NULL
            """)
    List<String> findTournamentCategoryCodesByPlayerId(@Param("playerId") long playerId);
}
