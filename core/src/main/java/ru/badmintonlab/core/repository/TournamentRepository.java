package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}
