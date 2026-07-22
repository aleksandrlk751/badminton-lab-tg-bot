package ru.badmintonlab.worker.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.entity.PlayerRatingHistory;
import ru.badmintonlab.core.entity.PlayerRatingId;
import ru.badmintonlab.core.repository.PlayerRatingHistoryRepository;
import ru.badmintonlab.core.repository.PlayerRatingRepository;
import ru.badmintonlab.core.repository.PlayerRepository;
import ru.badmintonlab.parser.model.PlayerProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Идемпотентный upsert профиля игрока: сам игрок, текущие рейтинги по дисциплинам
 * и история рейтинга (по дате).
 */
@Service
public class PlayerUpsertService {

    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository ratingRepository;
    private final PlayerRatingHistoryRepository historyRepository;

    public PlayerUpsertService(PlayerRepository playerRepository,
                               PlayerRatingRepository ratingRepository,
                               PlayerRatingHistoryRepository historyRepository) {
        this.playerRepository = playerRepository;
        this.ratingRepository = ratingRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void upsert(PlayerProfile profile) {
        Player player = playerRepository.findById(profile.id()).orElseGet(() -> new Player(profile.id()));
        player.setNick(profile.nick());
        applyFullName(player, profile.fullName());
        player.setCity(profile.city().orElse(null));
        player.setPlayingHand(mapHand(profile.playingHand().orElse(null)));
        player.setLastSeenAt(Instant.now());
        playerRepository.save(player);

        upsertRatings(profile.id(), profile.ratings());
        upsertHistories(profile.id(), profile.ratingHistories());
    }

    /**
     * Гарантирует наличие строки игрока (для FK), когда полного профиля ещё нет.
     */
    @Transactional
    public void ensurePlayer(long playerId, String nick) {
        if (playerRepository.existsById(playerId)) {
            return;
        }
        Player player = new Player(playerId);
        player.setNick(nick == null || nick.isBlank() ? String.valueOf(playerId) : nick);
        player.setLastSeenAt(Instant.now());
        playerRepository.save(player);
    }

    private void upsertRatings(long playerId, Map<ru.badmintonlab.parser.model.Discipline, BigDecimal> ratings) {
        for (var entry : ratings.entrySet()) {
            Discipline discipline = SnapshotSupport.toCore(entry.getKey());
            PlayerRatingId id = new PlayerRatingId(playerId, discipline);
            PlayerRating rating = ratingRepository.findById(id).orElseGet(() -> new PlayerRating(id, entry.getValue()));
            rating.setRating(entry.getValue());
            ratingRepository.save(rating);
        }
    }

    private void upsertHistories(long playerId,
                                Map<ru.badmintonlab.parser.model.Discipline, List<PlayerProfile.RatingPoint>> histories) {
        for (var entry : histories.entrySet()) {
            Discipline discipline = SnapshotSupport.toCore(entry.getKey());
            for (PlayerProfile.RatingPoint point : entry.getValue()) {
                historyRepository.findByPlayerIdAndDisciplineAndRecordedAt(playerId, discipline, point.date())
                        .ifPresentOrElse(
                                existing -> existing.setRating(point.rating()),
                                () -> historyRepository.save(new PlayerRatingHistory(
                                        playerId, discipline, point.date(), point.rating())));
            }
        }
    }

    private void applyFullName(Player player, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return;
        }
        String[] tokens = fullName.trim().split("\\s+");
        player.setLastName(tokens.length > 0 ? tokens[0] : null);
        player.setFirstName(tokens.length > 1 ? tokens[1] : null);
        player.setPatronymic(tokens.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(tokens, 2, tokens.length)) : null);
    }

    private String mapHand(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase();
        if (value.startsWith("прав")) {
            return "RIGHT";
        }
        if (value.startsWith("лев")) {
            return "LEFT";
        }
        return null;
    }
}
