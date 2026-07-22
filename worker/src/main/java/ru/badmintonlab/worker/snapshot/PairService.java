package ru.badmintonlab.worker.snapshot;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Pair;
import ru.badmintonlab.core.repository.PairRepository;

/**
 * Идемпотентное разрешение пары (get-or-create) с защитой от гонок между потоками слепка:
 * одну и ту же пару могут одновременно создавать разные турниры. Вставка выполняется в
 * отдельной транзакции ({@link PairInserter}), чтобы конфликт уникального ключа не ломал
 * внешнюю транзакцию, после чего пара до-читывается.
 */
@Service
public class PairService {

    private final PairRepository pairRepository;
    private final PairInserter pairInserter;

    public PairService(PairRepository pairRepository, PairInserter pairInserter) {
        this.pairRepository = pairRepository;
        this.pairInserter = pairInserter;
    }

    public Long getOrCreate(long a, long b, Discipline discipline) {
        long p1 = Math.min(a, b);
        long p2 = Math.max(a, b);
        return pairRepository.findByPlayer1IdAndPlayer2IdAndDiscipline(p1, p2, discipline)
                .map(Pair::getId)
                .orElseGet(() -> insertOrReread(p1, p2, discipline));
    }

    private Long insertOrReread(long p1, long p2, Discipline discipline) {
        try {
            return pairInserter.insert(p1, p2, discipline);
        } catch (DataIntegrityViolationException e) {
            // Пару успел создать другой поток — берём существующую.
            return pairRepository.findByPlayer1IdAndPlayer2IdAndDiscipline(p1, p2, discipline)
                    .map(Pair::getId)
                    .orElseThrow(() -> e);
        }
    }
}
