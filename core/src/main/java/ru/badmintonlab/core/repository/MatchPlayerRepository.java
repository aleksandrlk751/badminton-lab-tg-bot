package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.entity.MatchPlayer;
import ru.badmintonlab.core.entity.MatchPlayerId;

import java.util.Optional;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, MatchPlayerId> {

    /** Стадия последнего матча игрока на турнире (для подписи результата на карточке). */
    @Query(value = """
            SELECT m.stage FROM match m
            INNER JOIN match_player mp ON mp.match_id = m.id
            WHERE mp.player_id = :playerId AND m.tournament_id = :tournamentId
            ORDER BY m.played_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLastStageOnTournament(@Param("playerId") long playerId,
                                               @Param("tournamentId") long tournamentId);
}
