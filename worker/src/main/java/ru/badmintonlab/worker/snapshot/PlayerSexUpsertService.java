package ru.badmintonlab.worker.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.PlayerSex;
import ru.badmintonlab.core.domain.PlayerSexInference;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.repository.ParticipationRepository;
import ru.badmintonlab.core.repository.PlayerRatingRepository;
import ru.badmintonlab.core.repository.PlayerRepository;
import ru.badmintonlab.parser.model.PlayerDirectoryEntry;
import ru.badmintonlab.parser.model.PlayerProfileSexEvidence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Идемпотентная запись пола игрока: справочник — authoritative-источник;
 * fallback из дисциплин рейтинга и участий — только если {@code sex} ещё {@code null}.
 */
@Service
public class PlayerSexUpsertService {

    /** Ручные исключения, где эвристика по имени не применима (подтверждено вручную). */
    private static final Map<Long, PlayerSex> MANUAL_SEX_OVERRIDES = Map.of(
            24294L, PlayerSex.M
    );

    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository ratingRepository;
    private final ParticipationRepository participationRepository;
    private final PlayerUpsertService playerUpsertService;

    public PlayerSexUpsertService(PlayerRepository playerRepository,
                                    PlayerRatingRepository ratingRepository,
                                    ParticipationRepository participationRepository,
                                    PlayerUpsertService playerUpsertService) {
        this.playerRepository = playerRepository;
        this.ratingRepository = ratingRepository;
        this.participationRepository = participationRepository;
        this.playerUpsertService = playerUpsertService;
    }

    /**
     * Запись пола из справочника: перезаписывает существующее значение (источник authoritative).
     *
     * @return число игроков, у которых пол был установлен или изменён
     */
    @Transactional
    public int upsertFromDirectory(List<PlayerDirectoryEntry> entries, PlayerSex sex) {
        int updated = 0;
        for (PlayerDirectoryEntry entry : entries) {
            if (applyDirectorySex(entry, sex)) {
                updated++;
            }
        }
        return updated;
    }

    /**
     * Fallback по данным слепка r77: MS/MD/WS/WD из {@code player_rating} и локальных участий.
     */
    @Transactional
    public int inferMissingFromLocalDisciplines() {
        int updated = 0;
        for (Player player : playerRepository.findBySexIsNull()) {
            PlayerSex inferred = inferSexFromLocalData(player.getId());
            if (inferred != null) {
                player.setSex(inferred);
                playerRepository.save(player);
                updated++;
            }
        }
        return updated;
    }

    /**
     * Fallback по ФИО из локальной БД (офлайн, без сайта). Только если {@code sex IS NULL}.
     */
    @Transactional
    public int inferMissingFromNames() {
        int updated = 0;
        for (Player player : playerRepository.findBySexIsNull()) {
            PlayerSex inferred = inferSexFromNameFields(player);
            if (inferred != null) {
                player.setSex(inferred);
                playerRepository.save(player);
                updated++;
            }
        }
        return updated;
    }

    public List<Long> findPlayerIdsWithMissingSex() {
        return playerRepository.findBySexIsNull().stream().map(Player::getId).toList();
    }

    public PlayerSex inferFromProfileEvidence(PlayerProfileSexEvidence evidence) {
        List<Discipline> disciplines = evidence.ratingDisciplines().stream()
                .map(SnapshotSupport::toCore)
                .toList();
        PlayerSex fromDisciplines = PlayerSexInference.inferFromDisciplines(disciplines);
        if (fromDisciplines != null) {
            return fromDisciplines;
        }
        return PlayerSexInference.inferFromCategoryCodes(evidence.tournamentCategoryCodes());
    }

    @Transactional
    public boolean applyInferredSex(long playerId, PlayerSex sex) {
        return playerRepository.findById(playerId)
                .filter(player -> player.getSex() == null)
                .map(player -> {
                    player.setSex(sex);
                    player.setLastSeenAt(Instant.now());
                    playerRepository.save(player);
                    return true;
                })
                .orElse(false);
    }

    private boolean applyDirectorySex(PlayerDirectoryEntry entry, PlayerSex sex) {
        Player player = playerRepository.findById(entry.id()).orElseGet(() -> {
            playerUpsertService.ensurePlayer(entry.id(), entry.nick().orElse(null));
            return playerRepository.findById(entry.id()).orElseThrow();
        });
        entry.nick().ifPresent(nick -> {
            if (player.getNick().equals(String.valueOf(entry.id()))) {
                player.setNick(nick);
            }
        });
        if (player.getSex() == sex) {
            return false;
        }
        player.setSex(sex);
        player.setLastSeenAt(Instant.now());
        playerRepository.save(player);
        return true;
    }

    private PlayerSex inferSexFromLocalData(long playerId) {
        List<Discipline> disciplines = new ArrayList<>();
        for (PlayerRating rating : ratingRepository.findByIdPlayerId(playerId)) {
            disciplines.add(rating.getId().getDiscipline());
        }
        disciplines.addAll(participationRepository.findPairDisciplinesByPlayerId(playerId));

        PlayerSex fromDisciplines = PlayerSexInference.inferFromDisciplines(disciplines);
        if (fromDisciplines != null) {
            return fromDisciplines;
        }
        return PlayerSexInference.inferFromCategoryCodes(
                participationRepository.findTournamentCategoryCodesByPlayerId(playerId));
    }

    private PlayerSex inferSexFromNameFields(Player player) {
        PlayerSex manual = MANUAL_SEX_OVERRIDES.get(player.getId());
        if (manual != null) {
            return manual;
        }
        return PlayerSexInference.inferFromName(
                player.getFirstName(), player.getLastName(), player.getPatronymic());
    }
}
