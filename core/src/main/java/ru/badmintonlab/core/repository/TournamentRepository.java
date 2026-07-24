package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.badmintonlab.core.domain.TournamentStatus;
import ru.badmintonlab.core.entity.Tournament;

import java.time.Instant;
import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query("""
            SELECT t FROM Tournament t
            WHERE t.regionCode = :region
              AND t.status = :status
              AND t.startsAt >= :from
            ORDER BY t.startsAt ASC
            """)
    List<Tournament> findUpcoming(@Param("region") String region,
                                  @Param("status") TournamentStatus status,
                                  @Param("from") Instant from);
}
