package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.H2hResult;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.MatchSide;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.entity.PlayerRating;
import ru.badmintonlab.core.metrics.ForecastResult;
import ru.badmintonlab.core.metrics.ForecastService;
import ru.badmintonlab.core.metrics.FormService;
import ru.badmintonlab.core.metrics.PlayabilityIndexService;
import ru.badmintonlab.core.metrics.RatingDeltaEvent;
import ru.badmintonlab.core.repository.H2hRepository;
import ru.badmintonlab.core.repository.PlayerRatingRepository;
import ru.badmintonlab.core.repository.PlayerRepository;
import ru.badmintonlab.core.repository.projection.H2hMatchView;
import ru.badmintonlab.core.repository.projection.RatingDeltaView;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Head-to-head двух игроков: W-L, последние матчи, Form, прогноз P3 (рейтинг 👥 D).
 */
@Service
public class H2hService {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final int RECENT_LIMIT = 5;
    private static final Set<Discipline> PAIR_DISCIPLINES =
            EnumSet.of(Discipline.D, Discipline.MD, Discipline.WD, Discipline.XD);

    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final H2hRepository h2hRepository;
    private final H2hLazyFetchService lazyFetchService;
    private final FormService formService;
    private final PlayabilityIndexService playabilityIndexService;
    private final ForecastService forecastService;

    public H2hService(PlayerRepository playerRepository,
                      PlayerRatingRepository playerRatingRepository,
                      H2hRepository h2hRepository,
                      H2hLazyFetchService lazyFetchService,
                      FormService formService,
                      PlayabilityIndexService playabilityIndexService,
                      ForecastService forecastService) {
        this.playerRepository = playerRepository;
        this.playerRatingRepository = playerRatingRepository;
        this.h2hRepository = h2hRepository;
        this.lazyFetchService = lazyFetchService;
        this.formService = formService;
        this.playabilityIndexService = playabilityIndexService;
        this.forecastService = forecastService;
    }

    @Transactional
    public Optional<H2hResult> compare(long playerAId, long playerBId) {
        Optional<Player> optA = playerRepository.findById(playerAId);
        Optional<Player> optB = playerRepository.findById(playerBId);
        if (optA.isEmpty() || optB.isEmpty()) {
            return Optional.empty();
        }
        if (playerAId == playerBId) {
            return Optional.empty();
        }

        List<H2hMatchView> matches = loadMatches(playerAId, playerBId);
        int winsA = 0;
        int winsB = 0;
        List<Instant> meetingTimes = new ArrayList<>();
        List<H2hResult.H2hMatchLine> recent = new ArrayList<>();

        for (H2hMatchView m : matches) {
            boolean aWon = playerAWon(m);
            if (aWon) {
                winsA++;
            } else {
                winsB++;
            }
            meetingTimes.add(m.getPlayedAt());
            if (recent.size() < RECENT_LIMIT) {
                recent.add(toMatchLine(m, aWon));
            }
        }

        double formA = form(playerAId);
        double formB = form(playerBId);
        double s = playabilityIndexService.index(meetingTimes);
        double ratingA = ratingOrZero(playerAId, Discipline.D);
        double ratingB = ratingOrZero(playerBId, Discipline.D);
        ForecastResult forecast = forecastService.forecast(
                ratingA, ratingB, formA, formB, winsA, winsB, s);

        return Optional.of(new H2hResult(
                toSide(optA.get()),
                toSide(optB.get()),
                winsA,
                winsB,
                formA,
                formB,
                forecast,
                recent));
    }

    private List<H2hMatchView> loadMatches(long playerAId, long playerBId) {
        List<H2hMatchView> matches = h2hRepository.findHeadToHead(playerAId, playerBId);
        if (matches.isEmpty()) {
            lazyFetchService.fetchIfMissing(playerAId, playerBId);
            matches = h2hRepository.findHeadToHead(playerAId, playerBId);
        }
        return matches;
    }

    private double form(long playerId) {
        List<RatingDeltaEvent> events = h2hRepository.findRatingDeltas(playerId, PAIR_DISCIPLINES).stream()
                .map(this::toDeltaEvent)
                .toList();
        return formService.form(events);
    }

    private RatingDeltaEvent toDeltaEvent(RatingDeltaView view) {
        return new RatingDeltaEvent(view.getPlayedAt(), view.getRatingDelta().doubleValue());
    }

    private H2hResult.H2hMatchLine toMatchLine(H2hMatchView m, boolean playerAWon) {
        return new H2hResult.H2hMatchLine(
                m.getPlayedAt().atZone(MOSCOW).toLocalDate(),
                m.getTournamentName(),
                m.getScoreSets(),
                m.getRatingDelta(),
                playerAWon);
    }

    private H2hResult.H2hPlayerSide toSide(Player p) {
        return new H2hResult.H2hPlayerSide(
                p.getId(),
                Names.fullName(p.getLastName(), p.getFirstName(), p.getPatronymic()),
                p.getNick(),
                rating(p.getId(), Discipline.S),
                rating(p.getId(), Discipline.D));
    }

    private BigDecimal rating(long playerId, Discipline discipline) {
        return playerRatingRepository.findByIdPlayerId(playerId).stream()
                .filter(r -> r.getId().getDiscipline() == discipline)
                .map(PlayerRating::getRating)
                .findFirst()
                .orElse(null);
    }

    private double ratingOrZero(long playerId, Discipline discipline) {
        BigDecimal r = rating(playerId, discipline);
        return r == null ? 0.0 : r.doubleValue();
    }

    static boolean playerAWon(H2hMatchView m) {
        String[] parts = m.getScoreSets().split(":");
        int setsFirst = Integer.parseInt(parts[0].trim());
        int setsSecond = Integer.parseInt(parts[1].trim());
        boolean sideAWon = setsFirst > setsSecond;
        return (m.getPlayerSide() == MatchSide.A) == sideAWon;
    }
}
