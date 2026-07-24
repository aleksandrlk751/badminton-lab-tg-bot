package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.model.PartnerCandidateRow;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.bot.model.PartnerPickPage;
import ru.badmintonlab.bot.model.UpcomingTournamentRow;
import ru.badmintonlab.core.config.MetricsProperties;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.PairCompositionType;
import ru.badmintonlab.core.domain.PlayerSex;
import ru.badmintonlab.core.domain.TournamentDisciplineSupport;
import ru.badmintonlab.core.domain.TournamentStatus;
import ru.badmintonlab.core.entity.Player;
import ru.badmintonlab.core.entity.Tournament;
import ru.badmintonlab.core.metrics.GameAccentResult;
import ru.badmintonlab.core.metrics.PairCompositionService;
import ru.badmintonlab.core.metrics.PartnerScoreService;
import ru.badmintonlab.core.metrics.PlayabilityIndexService;
import ru.badmintonlab.core.repository.PartnerPickRepository;
import ru.badmintonlab.core.repository.PlayerRatingRepository;
import ru.badmintonlab.core.repository.PlayerRepository;
import ru.badmintonlab.core.repository.TournamentRegistrationRepository;
import ru.badmintonlab.core.repository.TournamentRepository;
import ru.badmintonlab.core.repository.projection.JointPartnershipView;
import ru.badmintonlab.core.repository.projection.PartnerCandidateView;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class PartnerPickService {

    private static final int MAX_CANDIDATES = 150;
    private static final int MAX_DISPLAY = 12;
    private static final String REGION = "r77";

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final PartnerPickRepository partnerPickRepository;
    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final PlayerFormService playerFormService;
    private final PlayerGameAccentService playerGameAccentService;
    private final PairCompositionService pairCompositionService;
    private final PartnerScoreService partnerScoreService;
    private final PlayabilityIndexService playabilityIndexService;
    private final MetricsProperties metricsProperties;

    public PartnerPickService(TournamentRepository tournamentRepository,
                              TournamentRegistrationRepository registrationRepository,
                              PartnerPickRepository partnerPickRepository,
                              PlayerRepository playerRepository,
                              PlayerRatingRepository playerRatingRepository,
                              PlayerFormService playerFormService,
                              PlayerGameAccentService playerGameAccentService,
                              PairCompositionService pairCompositionService,
                              PartnerScoreService partnerScoreService,
                              PlayabilityIndexService playabilityIndexService,
                              MetricsProperties metricsProperties) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.partnerPickRepository = partnerPickRepository;
        this.playerRepository = playerRepository;
        this.playerRatingRepository = playerRatingRepository;
        this.playerFormService = playerFormService;
        this.playerGameAccentService = playerGameAccentService;
        this.pairCompositionService = pairCompositionService;
        this.partnerScoreService = partnerScoreService;
        this.playabilityIndexService = playabilityIndexService;
        this.metricsProperties = metricsProperties;
    }

    @Transactional(readOnly = true)
    public List<UpcomingTournamentRow> upcomingTournaments() {
        List<Tournament> list = tournamentRepository.findUpcoming(
                REGION, TournamentStatus.UPCOMING, Instant.now());
        List<UpcomingTournamentRow> rows = new ArrayList<>();
        for (Tournament t : list) {
            long pairs = registrationRepository.findByIdTournamentId(t.getId()).stream()
                    .filter(r -> r.getPairId() != null)
                    .map(r -> r.getPairId())
                    .distinct()
                    .count();
            rows.add(new UpcomingTournamentRow(
                    t.getId(), t.getName(), t.getStartsAt(), t.getRatingLimit(), (int) pairs));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public Optional<PartnerPickPage> pick(long tournamentId, long userId) {
        Optional<Tournament> tournamentOpt = tournamentRepository.findById(tournamentId);
        Optional<Player> userOpt = playerRepository.findById(userId);
        if (tournamentOpt.isEmpty() || userOpt.isEmpty()) {
            return Optional.empty();
        }
        Tournament tournament = tournamentOpt.get();
        Player user = userOpt.get();
        if (user.getSex() == null) {
            return Optional.empty();
        }

        Discipline pairDiscipline = TournamentDisciplineSupport.pairDiscipline(
                tournament.getCategoryCode(), tournament.getName());
        double userRating = resolvePairRating(userId);
        Double limit = tournament.getRatingLimit() != null
                ? tournament.getRatingLimit().doubleValue()
                : null;

        Set<Long> excluded = registrationRepository.findPlayerIdsInConfirmedPairs(tournamentId);
        List<PlayerSex> allowedSexes = allowedPartnerSexes(user.getSex(), pairDiscipline);
        if (allowedSexes.isEmpty()) {
            return Optional.empty();
        }

        List<PartnerCandidateView> raw = partnerPickRepository.findCandidates(
                userId,
                userRating,
                limit,
                Discipline.D,
                allowedSexes,
                excluded.size(),
                excluded.isEmpty() ? List.of() : excluded.stream().toList());

        if (raw.size() > MAX_CANDIDATES) {
            raw = raw.subList(0, MAX_CANDIDATES);
        }

        List<Long> partnerIds = raw.stream().map(PartnerCandidateView::getPlayerId).toList();
        Map<Long, JointStats> jointStats = loadJointStats(userId, partnerIds);

        PairCompositionType tournamentComposition =
                TournamentDisciplineSupport.compositionForTournament(pairDiscipline);
        Instant historySince = Instant.now().minus(
                metricsProperties.partnerHistoryMonths() * 30L, ChronoUnit.DAYS);

        List<ScoredCandidate> scored = new ArrayList<>();
        for (PartnerCandidateView c : raw) {
            double candidateRating = c.getRating().doubleValue();
            JointStats joint = jointStats.getOrDefault(c.getPlayerId(), JointStats.EMPTY);
            boolean successful = joint.positiveDeltaSince(historySince);
            double playability = playabilityIndexService.index(joint.meetingTimes());

            var scoreResult = partnerScoreService.score(new PartnerScoreService.Input(
                    userRating,
                    candidateRating,
                    limit,
                    joint.deltaSumSince(historySince),
                    playability,
                    successful));

            Optional<GameAccentResult> accent = playerGameAccentService.accentForCard(c.getPlayerId());
            boolean categoryMatch = accent.isPresent()
                    && tournamentComposition != null
                    && tournamentComposition == accent.get().preferenceType();
            boolean goodForm = playerFormService.formIfKnown(c.getPlayerId()).orElse(0) > 0;
            PairCompositionType futurePair = pairCompositionService.resolve(user.getSex(), c.getSex());
            boolean ideal = successful && categoryMatch && goodForm;

            scored.add(new ScoredCandidate(
                    c,
                    candidateRating,
                    scoreResult.score(),
                    successful,
                    categoryMatch,
                    goodForm,
                    ideal,
                    futurePair));
        }

        Comparator<ScoredCandidate> cmp = Comparator
                .comparing(ScoredCandidate::categoryMatch).reversed()
                .thenComparing(ScoredCandidate::score).reversed()
                .thenComparing(c -> c.candidate().getNick());

        List<PartnerCandidateRow> successfulRows = scored.stream()
                .filter(ScoredCandidate::successfulHistory)
                .sorted(cmp)
                .limit(MAX_DISPLAY)
                .map(s -> toRow(s, userRating))
                .toList();

        List<PartnerCandidateRow> newcomerRows = scored.stream()
                .filter(s -> !s.successfulHistory())
                .sorted(cmp)
                .limit(MAX_DISPLAY)
                .map(s -> toRow(s, userRating))
                .toList();

        String userLabel = formatUserLabel(user);
        return Optional.of(new PartnerPickPage(
                tournamentId,
                tournament.getName(),
                tournament.getStartsAt(),
                tournament.getRatingLimit(),
                userId,
                userLabel,
                userRating,
                successfulRows,
                newcomerRows));
    }

    private PartnerCandidateRow toRow(ScoredCandidate s, double userRating) {
        var c = s.candidate();
        double avg = (userRating + s.rating()) / 2.0;
        String fullName = Names.fullName(c.getLastName(), c.getFirstName(), c.getPatronymic());
        return new PartnerCandidateRow(
                c.getPlayerId(),
                fullName,
                c.getNick(),
                c.getCity(),
                s.rating(),
                avg,
                s.score(),
                s.successfulHistory(),
                s.categoryMatch(),
                s.goodForm(),
                s.ideal(),
                s.futurePairType());
    }

    private String formatUserLabel(Player user) {
        String name = user.getLastName() != null ? user.getLastName() : "";
        if (user.getFirstName() != null) {
            name = (name + " " + user.getFirstName()).trim();
        }
        if (name.isBlank()) {
            return user.getNick();
        }
        return name + " (" + user.getNick() + ")";
    }

    private Map<Long, JointStats> loadJointStats(long userId, List<Long> partnerIds) {
        if (partnerIds.isEmpty()) {
            return Map.of();
        }
        List<JointPartnershipView> rows = partnerPickRepository.findJointPartnerships(userId, partnerIds);
        Map<Long, JointStats> map = new HashMap<>();
        for (JointPartnershipView row : rows) {
            map.computeIfAbsent(row.getPartnerId(), id -> new JointStats())
                    .add(row.getTournamentStartsAt(), row.getUserDelta(), row.getPartnerDelta());
        }
        return map;
    }

    /** Парный рейтинг ЛАБ — единая шкала {@link Discipline#D}, без MD/WD/XD в {@code player_rating}. */
    private double resolvePairRating(long playerId) {
        return playerRatingRepository.findById(
                        new ru.badmintonlab.core.entity.PlayerRatingId(playerId, Discipline.D))
                .map(r -> r.getRating().doubleValue())
                .orElse(0.0);
    }

    private List<PlayerSex> allowedPartnerSexes(PlayerSex userSex, Discipline pairDiscipline) {
        return switch (pairDiscipline) {
            case MD -> List.of(PlayerSex.M);
            case WD -> List.of(PlayerSex.F);
            case XD -> userSex == PlayerSex.M ? List.of(PlayerSex.F)
                    : userSex == PlayerSex.F ? List.of(PlayerSex.M) : List.of();
            default -> EnumSet.allOf(PlayerSex.class).stream().toList();
        };
    }

    private record ScoredCandidate(
            PartnerCandidateView candidate,
            double rating,
            double score,
            boolean successfulHistory,
            boolean categoryMatch,
            boolean goodForm,
            boolean ideal,
            PairCompositionType futurePairType
    ) {}

    private static final class JointStats {
        static final JointStats EMPTY = new JointStats();
        private final List<Instant> meetingTimes = new ArrayList<>();
        private final List<DeltaAt> deltas = new ArrayList<>();

        void add(Instant at, BigDecimal userDelta, BigDecimal partnerDelta) {
            meetingTimes.add(at);
            double u = userDelta != null ? userDelta.doubleValue() : 0;
            double p = partnerDelta != null ? partnerDelta.doubleValue() : 0;
            deltas.add(new DeltaAt(at, u + p));
        }

        List<Instant> meetingTimes() {
            return meetingTimes;
        }

        double deltaSumSince(Instant since) {
            return deltas.stream()
                    .filter(d -> !d.at.isBefore(since))
                    .mapToDouble(d -> d.sum)
                    .sum();
        }

        boolean positiveDeltaSince(Instant since) {
            return deltaSumSince(since) > 0;
        }

        private record DeltaAt(Instant at, double sum) {}
    }
}
