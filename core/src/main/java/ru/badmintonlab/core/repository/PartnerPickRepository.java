package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.PlayerSex;
import ru.badmintonlab.core.entity.Participation;
import ru.badmintonlab.core.repository.projection.JointPartnershipView;
import ru.badmintonlab.core.repository.projection.PartnerCandidateView;

import java.util.Collection;
import java.util.List;

public interface PartnerPickRepository extends JpaRepository<Participation, Long> {

    /** {@code ratingDiscipline} — всегда {@link Discipline#D} (единый парный рейтинг ЛАБ). */

    @Query("""
            SELECT p.id AS playerId, p.nick AS nick,
                   p.lastName AS lastName, p.firstName AS firstName, p.patronymic AS patronymic,
                   p.city AS city, pr.rating AS rating, p.sex AS sex
            FROM Player p
            JOIN PlayerRating pr ON pr.id.playerId = p.id AND pr.id.discipline = :ratingDiscipline
            WHERE p.id <> :userId
              AND p.sex IS NOT NULL
              AND p.sex IN :allowedSexes
              AND (:excludeCount = 0 OR p.id NOT IN :excludedIds)
              AND (:limit IS NULL OR (pr.rating + :userRating) / 2 <= :limit)
              AND (:maxPlayerLimit IS NULL OR (pr.rating <= :maxPlayerLimit AND :userRating <= :maxPlayerLimit))
            ORDER BY pr.rating DESC
            """)
    List<PartnerCandidateView> findCandidates(@Param("userId") long userId,
                                              @Param("userRating") double userRating,
                                              @Param("limit") Double limit,
                                              @Param("maxPlayerLimit") Double maxPlayerLimit,
                                              @Param("ratingDiscipline") Discipline ratingDiscipline,
                                              @Param("allowedSexes") Collection<PlayerSex> allowedSexes,
                                              @Param("excludeCount") int excludeCount,
                                              @Param("excludedIds") Collection<Long> excludedIds);

    /**
     * Игроки с хотя бы одним совместным парным турниром с {@code userId}; те же фильтры лимита/пола/регистрации,
     * без обрезки по рейтингу — для блока «Уже играли».
     */
    @Query("""
            SELECT p.id AS playerId, p.nick AS nick,
                   p.lastName AS lastName, p.firstName AS firstName, p.patronymic AS patronymic,
                   p.city AS city, pr.rating AS rating, p.sex AS sex
            FROM Player p
            JOIN PlayerRating pr ON pr.id.playerId = p.id AND pr.id.discipline = :ratingDiscipline
            WHERE p.id <> :userId
              AND p.sex IS NOT NULL
              AND p.sex IN :allowedSexes
              AND (:excludeCount = 0 OR p.id NOT IN :excludedIds)
              AND (:limit IS NULL OR (pr.rating + :userRating) / 2 <= :limit)
              AND (:maxPlayerLimit IS NULL OR (pr.rating <= :maxPlayerLimit AND :userRating <= :maxPlayerLimit))
              AND EXISTS (
                SELECT 1 FROM Participation pa1
                JOIN Participation pa2 ON pa1.pairId = pa2.pairId AND pa1.pairId IS NOT NULL
                WHERE pa1.playerId = :userId AND pa2.playerId = p.id
              )
            """)
    List<PartnerCandidateView> findFormerPartners(@Param("userId") long userId,
                                              @Param("userRating") double userRating,
                                              @Param("limit") Double limit,
                                              @Param("maxPlayerLimit") Double maxPlayerLimit,
                                              @Param("ratingDiscipline") Discipline ratingDiscipline,
                                              @Param("allowedSexes") Collection<PlayerSex> allowedSexes,
                                              @Param("excludeCount") int excludeCount,
                                              @Param("excludedIds") Collection<Long> excludedIds);

    @Query("""
            SELECT pa2.playerId AS partnerId,
                   t.startsAt AS tournamentStartsAt,
                   pa1.ratingDelta AS userDelta,
                   pa2.ratingDelta AS partnerDelta
            FROM Participation pa1
            JOIN Participation pa2 ON pa1.pairId = pa2.pairId AND pa1.pairId IS NOT NULL
            JOIN Tournament t ON t.id = pa1.tournamentId
            WHERE pa1.playerId = :userId
              AND pa2.playerId IN :partnerIds
            ORDER BY t.startsAt DESC
            """)
    List<JointPartnershipView> findJointPartnerships(@Param("userId") long userId,
                                                     @Param("partnerIds") Collection<Long> partnerIds);
}
