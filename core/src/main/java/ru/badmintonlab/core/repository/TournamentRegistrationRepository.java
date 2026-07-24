package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.entity.TournamentRegistration;
import ru.badmintonlab.core.entity.TournamentRegistrationId;

import java.util.List;
import java.util.Set;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, TournamentRegistrationId> {

    @Query("""
            SELECT tr.id.playerId
            FROM TournamentRegistration tr
            WHERE tr.id.tournamentId = :tournamentId AND tr.pairId IS NOT NULL
            """)
    Set<Long> findPlayerIdsInConfirmedPairs(@Param("tournamentId") long tournamentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TournamentRegistration tr WHERE tr.id.tournamentId = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") long tournamentId);

    List<TournamentRegistration> findByIdTournamentId(Long tournamentId);
}
