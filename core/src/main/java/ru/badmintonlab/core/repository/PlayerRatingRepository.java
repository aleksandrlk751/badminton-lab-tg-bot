package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.entity.PlayerRatingId;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, PlayerRatingId> {
}
