package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Pair;

import java.util.Optional;

public interface PairRepository extends JpaRepository<Pair, Long> {

    Optional<Pair> findByPlayer1IdAndPlayer2IdAndDiscipline(Long player1Id, Long player2Id, Discipline discipline);
}
