package ru.badmintonlab.bot.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.repository.RivalSummaryRepository;
import ru.badmintonlab.core.repository.projection.DisciplineGamesView;
import ru.badmintonlab.core.repository.projection.RivalView;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Экран «Соперники» (этап 4): топ по числу встреч, выбор дисциплины, пагинация.
 */
@Service
public class RivalService {

    public static final int PAGE_SIZE = 8;

    /** Парные дисциплины MVP — приоритет при выборе дисциплины по умолчанию. */
    private static final Set<Discipline> PAIR_DISCIPLINES =
            Set.of(Discipline.D, Discipline.MD, Discipline.WD, Discipline.XD);

    private final RivalSummaryRepository rivalSummaryRepository;

    public RivalService(RivalSummaryRepository rivalSummaryRepository) {
        this.rivalSummaryRepository = rivalSummaryRepository;
    }

    /**
     * Дисциплины игрока с встречами, по убыванию числа встреч (для переключателя).
     */
    @Transactional(readOnly = true)
    public List<Discipline> disciplinesWithRivals(long playerId) {
        return rivalSummaryRepository.disciplineGames(playerId).stream()
                .map(DisciplineGamesView::getDiscipline)
                .toList();
    }

    /**
     * Дисциплина по умолчанию: парная (D/MD/WD/XD) с максимумом встреч; иначе — самая активная.
     */
    @Transactional(readOnly = true)
    public Optional<Discipline> defaultDiscipline(long playerId) {
        List<Discipline> ordered = disciplinesWithRivals(playerId);
        return ordered.stream()
                .filter(PAIR_DISCIPLINES::contains)
                .findFirst()
                .or(() -> ordered.stream().findFirst());
    }

    @Transactional(readOnly = true)
    public RivalsPage rivals(long playerId, Discipline discipline, int page) {
        int safePage = Math.max(0, page);
        long total = rivalSummaryRepository.countByIdPlayerIdAndIdDiscipline(playerId, discipline);
        List<RivalRow> rows = rivalSummaryRepository
                .findTopRivals(playerId, discipline, PageRequest.of(safePage, PAGE_SIZE)).stream()
                .map(this::toRow)
                .toList();
        return new RivalsPage(playerId, discipline, rows, safePage, PAGE_SIZE, total,
                disciplinesWithRivals(playerId));
    }

    private RivalRow toRow(RivalView v) {
        return new RivalRow(
                v.getOpponentId(),
                v.getNick(),
                Names.fullName(v.getLastName(), v.getFirstName(), null),
                v.getCity(),
                v.getWins() == null ? 0 : v.getWins(),
                v.getLosses() == null ? 0 : v.getLosses());
    }
}
