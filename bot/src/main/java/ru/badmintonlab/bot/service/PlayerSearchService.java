package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.repository.PlayerRepository;

import java.util.List;

/**
 * Нечёткий поиск игрока по нику/ФИО (этап 4).
 */
@Service
public class PlayerSearchService {

    /** Минимальная длина запроса (bg: pg_trgm устойчив от 3 символов). */
    public static final int MIN_QUERY_LENGTH = 3;

    /** Максимум результатов в выдаче (BRIEF §5: до 5–10). */
    public static final int MAX_RESULTS = 10;

    private final PlayerRepository playerRepository;

    public PlayerSearchService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
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
        return new PlayerSearchResult(
                p.getId(),
                p.getNick(),
                Names.fullName(p.getLastName(), p.getFirstName(), p.getPatronymic()),
                p.getCity());
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
