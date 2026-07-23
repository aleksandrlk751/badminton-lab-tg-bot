package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Match;
import ru.badmintonlab.core.repository.projection.GameAccentMatchView;
import ru.badmintonlab.core.repository.projection.H2hMatchView;
import ru.badmintonlab.core.repository.projection.RatingDeltaView;

import java.util.Collection;
import java.util.List;

/**
 * Запросы head-to-head и дельт для метрик (вариант C — из {@code match}/{@code match_player}).
 */
public interface H2hRepository extends JpaRepository<Match, Long> {

    @Query("""
            SELECT m.id AS matchId, m.playedAt AS playedAt, m.scoreSets AS scoreSets, m.stage AS stage,
                   m.discipline AS discipline, t.name AS tournamentName,
                   mpA.side AS playerSide, mpA.ratingDelta AS ratingDelta
            FROM Match m
            JOIN Tournament t ON t.id = m.tournamentId
            JOIN MatchPlayer mpA ON mpA.id.matchId = m.id AND mpA.id.playerId = :playerA
            JOIN MatchPlayer mpB ON mpB.id.matchId = m.id AND mpB.id.playerId = :playerB
            WHERE mpA.side <> mpB.side
            ORDER BY m.playedAt DESC
            """)
    List<H2hMatchView> findHeadToHead(@Param("playerA") long playerA, @Param("playerB") long playerB);

    @Query("""
            SELECT COUNT(m) > 0
            FROM Match m
            JOIN MatchPlayer mpA ON mpA.id.matchId = m.id AND mpA.id.playerId = :playerA
            JOIN MatchPlayer mpB ON mpB.id.matchId = m.id AND mpB.id.playerId = :playerB
            WHERE m.tournamentId = :tournamentId AND mpA.side <> mpB.side
            """)
    boolean hasHeadToHeadInTournament(@Param("playerA") long playerA,
                                      @Param("playerB") long playerB,
                                      @Param("tournamentId") long tournamentId);

    @Query("""
            SELECT m.playedAt AS playedAt, mp.ratingDelta AS ratingDelta
            FROM MatchPlayer mp
            JOIN Match m ON m.id = mp.id.matchId
            WHERE mp.id.playerId = :playerId
              AND m.discipline IN :disciplines
              AND mp.ratingDelta IS NOT NULL
            ORDER BY m.playedAt DESC
            """)
    List<RatingDeltaView> findRatingDeltas(@Param("playerId") long playerId,
                                           @Param("disciplines") Collection<Discipline> disciplines);

    /**
     * Парные матчи игрока с полом партнёра на той же стороне — для {@link ru.badmintonlab.core.metrics.GameAccentService}.
     */
    @Query("""
            SELECT m.playedAt AS playedAt, mp.ratingDelta AS ratingDelta,
                   p.sex AS playerSex, partner.sex AS partnerSex
            FROM MatchPlayer mp
            JOIN Match m ON m.id = mp.id.matchId
            JOIN Player p ON p.id = mp.id.playerId
            JOIN MatchPlayer mpPartner ON mpPartner.id.matchId = m.id
                AND mpPartner.side = mp.side AND mpPartner.id.playerId <> mp.id.playerId
            JOIN Player partner ON partner.id = mpPartner.id.playerId
            WHERE mp.id.playerId = :playerId
              AND m.discipline IN :disciplines
              AND mp.ratingDelta IS NOT NULL
            ORDER BY m.playedAt DESC
            """)
    List<GameAccentMatchView> findAccentMatches(@Param("playerId") long playerId,
                                                @Param("disciplines") Collection<Discipline> disciplines);
}
