package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.PlayerRatingHistory;

import java.time.LocalDate;
import java.util.Optional;

public interface PlayerRatingHistoryRepository extends JpaRepository<PlayerRatingHistory, Long> {

    Optional<PlayerRatingHistory> findByPlayerIdAndDisciplineAndRecordedAt(
            Long playerId, Discipline discipline, LocalDate recordedAt);
}
