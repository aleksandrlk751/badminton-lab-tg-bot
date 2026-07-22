package ru.badmintonlab.bot.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.repository.PlayerRepository;
import ru.badmintonlab.core.repository.RivalSummaryRepository;
import ru.badmintonlab.core.repository.projection.DisciplineGamesView;
import ru.badmintonlab.core.repository.projection.RivalView;

import java.util.List;
import java.util.Set;

/**
 * Экран «Соперники»: фильтр «Все» (сумма W–L) или разряд, пагинация.
 */
@Service
public class RivalService {

    public static final int PAGE_SIZE = 8;

    private final RivalSummaryRepository rivalSummaryRepository;
    private final PlayerRepository playerRepository;

    public RivalService(RivalSummaryRepository rivalSummaryRepository,
                        PlayerRepository playerRepository) {
        this.rivalSummaryRepository = rivalSummaryRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<Discipline> disciplinesWithRivals(long playerId) {
        return rivalSummaryRepository.disciplineGames(playerId).stream()
                .map(DisciplineGamesView::getDiscipline)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasRivals(long playerId) {
        return !disciplinesWithRivals(playerId).isEmpty();
    }

    /**
     * @param discipline {@code null} — фильтр «Все» (агрегат по разрядам).
     */
    @Transactional(readOnly = true)
    public RivalsPage rivals(long playerId, Discipline discipline, int page) {
        int safePage = Math.max(0, page);
        String playerFullName = playerRepository.findById(playerId)
                .map(p -> Names.fullName(p.getLastName(), p.getFirstName(), p.getPatronymic()))
                .orElse("");
        List<Discipline> available = disciplinesWithRivals(playerId);

        long total;
        List<RivalRow> rows;
        if (discipline == null) {
            total = rivalSummaryRepository.countDistinctOpponents(playerId);
            rows = rivalSummaryRepository
                    .findTopRivalsAllDisciplines(playerId, PageRequest.of(safePage, PAGE_SIZE)).stream()
                    .map(this::toRow)
                    .toList();
        } else {
            total = rivalSummaryRepository.countByIdPlayerIdAndIdDiscipline(playerId, discipline);
            rows = rivalSummaryRepository
                    .findTopRivals(playerId, discipline, PageRequest.of(safePage, PAGE_SIZE)).stream()
                    .map(this::toRow)
                    .toList();
        }
        return new RivalsPage(playerId, playerFullName, discipline, rows, safePage, PAGE_SIZE, total, available);
    }

    private RivalRow toRow(RivalView v) {
        int wins = toInt(v.getWins());
        int losses = toInt(v.getLosses());
        return new RivalRow(
                v.getOpponentId(),
                v.getNick(),
                Names.fullName(v.getLastName(), v.getFirstName(), null),
                v.getCity(),
                wins,
                losses);
    }

    private static int toInt(Number value) {
        return value == null ? 0 : value.intValue();
    }
}
