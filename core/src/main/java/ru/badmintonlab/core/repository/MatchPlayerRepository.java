package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.MatchPlayer;
import ru.badmintonlab.core.entity.MatchPlayerId;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, MatchPlayerId> {
}
