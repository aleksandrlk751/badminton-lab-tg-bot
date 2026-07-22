package ru.badmintonlab.worker.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Pair;
import ru.badmintonlab.core.repository.PairRepository;

/**
 * Вставка пары в отдельной транзакции (REQUIRES_NEW). Выделено в самостоятельный бин,
 * чтобы вызов проходил через прокси Spring и новая транзакция действительно создавалась
 * (иначе self-invocation в {@link PairService} обошёл бы прокси).
 */
@Service
public class PairInserter {

    private final PairRepository pairRepository;

    public PairInserter(PairRepository pairRepository) {
        this.pairRepository = pairRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long insert(long p1, long p2, Discipline discipline) {
        return pairRepository.save(new Pair(p1, p2, discipline)).getId();
    }
}
