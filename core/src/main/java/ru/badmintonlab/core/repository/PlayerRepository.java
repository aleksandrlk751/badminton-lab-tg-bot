package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
