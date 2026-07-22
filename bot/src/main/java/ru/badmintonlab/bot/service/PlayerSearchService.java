package ru.badmintonlab.bot.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.repository.PlayerRatingRepository;
import ru.badmintonlab.core.repository.PlayerRepository;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Нечёткий поиск игрока по нику/ФИО (этап 4).
 */
@Service
public class PlayerSearchService implements PlayerSearchOperations {

    public static final int MIN_QUERY_LENGTH = 3;
    public static final int MAX_RESULTS = 10;

    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository playerRatingRepository;

    public PlayerSearchService(PlayerRepository playerRepository,
                               PlayerRatingRepository playerRatingRepository) {
        this.playerRepository = playerRepository;
        this.playerRatingRepository = playerRatingRepository;
    }

    public boolean isQueryTooShort(String rawQuery) {
        return normalize(rawQuery).length() < MIN_QUERY_LENGTH;
    }

    @Transactional(readOnly = true)
    public List<PlayerSearchResult> search(String rawQuery) {
        String query = normalize(rawQuery);
        if (query.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        return playerRepository.search(query, MAX_RESULTS).stream()
                .map(this::toResult)
                .toList();
    }

    private PlayerSearchResult toResult(Player p) {
        Map<Discipline, BigDecimal> ratings = ratingsByDiscipline(p.getId());
        return new PlayerSearchResult(
                p.getId(),
                p.getNick(),
                p.getLastName(),
                p.getFirstName(),
                p.getPatronymic(),
                p.getCity(),
                ratings.get(Discipline.S),
                ratings.get(Discipline.D));
    }

    private Map<Discipline, BigDecimal> ratingsByDiscipline(long playerId) {
        Map<Discipline, BigDecimal> map = new EnumMap<>(Discipline.class);
        for (PlayerRating r : playerRatingRepository.findByIdPlayerId(playerId)) {
            Discipline d = r.getId().getDiscipline();
            if (d == Discipline.S || d == Discipline.D) {
                map.put(d, r.getRating());
            }
        }
        return map;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
