package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.RivalSummary;
import ru.badmintonlab.core.entity.RivalSummaryId;

public interface RivalSummaryRepository extends JpaRepository<RivalSummary, RivalSummaryId> {
}
