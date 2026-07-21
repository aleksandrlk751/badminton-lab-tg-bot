package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
