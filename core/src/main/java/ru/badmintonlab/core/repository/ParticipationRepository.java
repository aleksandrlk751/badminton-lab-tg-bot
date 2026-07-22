package ru.badmintonlab.core.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.entity.Participation;
import ru.badmintonlab.core.repository.projection.LastTournamentView;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    /**
     * Последние турниры игрока по дате старта (для карточки берём первый).
     * Participation не имеет ассоциации к Tournament (FK — обычный столбец), поэтому явный join в JPQL.
     */
    @Query("""
            SELECT t.name AS name, t.startsAt AS startsAt, pa.place AS place
            FROM Participation pa, Tournament t
            WHERE t.id = pa.tournamentId AND pa.playerId = :playerId
            ORDER BY t.startsAt DESC
            """)
    List<LastTournamentView> findLastTournaments(@Param("playerId") Long playerId, Pageable pageable);
}
