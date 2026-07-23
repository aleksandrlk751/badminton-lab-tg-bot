package ru.badmintonlab.bot.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.LastTournamentInfo;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.RatingLine;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.bot.util.TournamentResults;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.repository.MatchPlayerRepository;
import ru.badmintonlab.core.repository.ParticipationRepository;
import ru.badmintonlab.core.repository.PlayerRatingRepository;
import ru.badmintonlab.core.repository.PlayerRepository;
import ru.badmintonlab.core.repository.projection.LastTournamentView;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сборка карточки игрока: профиль, рейтинги S/D, последний турнир.
 */
@Service
public class PlayerCardService implements PlayerCardLoader {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final List<Discipline> CARD_DISCIPLINES = List.of(Discipline.S, Discipline.D);

    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final ParticipationRepository participationRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final SnapshotInfoService snapshotInfoService;
    private final PlayerFormService playerFormService;
    private final PlayerStabilityService playerStabilityService;
    private final PlayerGameAccentService playerGameAccentService;

    public PlayerCardService(PlayerRepository playerRepository,
                             PlayerRatingRepository playerRatingRepository,
                             ParticipationRepository participationRepository,
                             MatchPlayerRepository matchPlayerRepository,
                             SnapshotInfoService snapshotInfoService,
                             PlayerFormService playerFormService,
                             PlayerStabilityService playerStabilityService,
                             PlayerGameAccentService playerGameAccentService) {
        this.playerRepository = playerRepository;
        this.playerRatingRepository = playerRatingRepository;
        this.participationRepository = participationRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.snapshotInfoService = snapshotInfoService;
        this.playerFormService = playerFormService;
        this.playerStabilityService = playerStabilityService;
        this.playerGameAccentService = playerGameAccentService;
    }

    @Transactional(readOnly = true)
    public Optional<PlayerCard> card(long playerId) {
        return playerRepository.findById(playerId).map(this::buildCard);
    }

    private PlayerCard buildCard(Player p) {
        return new PlayerCard(
                p.getId(),
                p.getNick(),
                Names.fullName(p.getLastName(), p.getFirstName(), p.getPatronymic()),
                p.getCity(),
                ratings(p.getId()),
                formValue(p.getId()),
                playerStabilityService.stabilityEmojiForCard(p.getId()).orElse(null),
                playerGameAccentService.accentForCard(p.getId()).orElse(null),
                lastTournament(p.getId()),
                snapshotInfoService.lastSnapshotDate().orElse(null));
    }

    private Double formValue(long playerId) {
        return playerFormService.formForCard(playerId);
    }

    private List<RatingLine> ratings(long playerId) {
        Map<Discipline, PlayerRating> byDiscipline = new EnumMap<>(Discipline.class);
        for (PlayerRating r : playerRatingRepository.findByIdPlayerId(playerId)) {
            byDiscipline.put(r.getId().getDiscipline(), r);
        }
        List<RatingLine> lines = new ArrayList<>();
        for (Discipline d : CARD_DISCIPLINES) {
            PlayerRating r = byDiscipline.get(d);
            if (r != null) {
                lines.add(new RatingLine(d, r.getRating()));
            }
        }
        return lines;
    }

    private LastTournamentInfo lastTournament(long playerId) {
        List<LastTournamentView> last =
                participationRepository.findLastTournaments(playerId, PageRequest.of(0, 1));
        if (last.isEmpty()) {
            return null;
        }
        LastTournamentView v = last.get(0);
        String stage = v.getTournamentId() == null
                ? null
                : matchPlayerRepository.findLastStageOnTournament(playerId, v.getTournamentId()).orElse(null);
        String resultLabel = TournamentResults.label(v.getPlace(), stage);
        return new LastTournamentInfo(
                v.getName(),
                v.getStartsAt() == null ? null : v.getStartsAt().atZone(MOSCOW).toLocalDate(),
                v.getPlace(),
                resultLabel);
    }
}
