package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.entity.PlayerRatingId;

import java.util.List;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, PlayerRatingId> {

    List<PlayerRating> findByIdPlayerId(Long playerId);
}
