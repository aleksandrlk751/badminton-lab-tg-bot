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
import ru.badmintonlab.core.metrics.PlayabilityIndexService.TimedValue;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartnerPickService {

    private static final int MAX_CANDIDATES = 150;
    private static final int MAX_DISPLAY = 5;
    private static final String REGION = "r77";

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final PartnerPickRepository partnerPickRepository;
    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final PlayerFormService playerFormService;
    private final PlayerGameAccentService playerGameAccentService;
    private final PlayerStabilityService playerStabilityService;
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
                              PlayerStabilityService playerStabilityService,
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
        this.playerStabilityService = playerStabilityService;
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
        Double pairLimit = tournament.getRatingLimit() != null
                ? tournament.getRatingLimit().doubleValue()
                : null;
        Double maxPlayerLimit = tournament.getMaxPlayerRatingLimit() != null
                ? tournament.getMaxPlayerRatingLimit().doubleValue()
                : null;

        if (!fitsTournamentRating(userRating, maxPlayerLimit)) {
            return Optional.empty();
        }

        Set<Long> excluded = registrationRepository.findPlayerIdsInConfirmedPairs(tournamentId);
        List<PlayerSex> allowedSexes = allowedPartnerSexes(user.getSex(), pairDiscipline);
        if (allowedSexes.isEmpty()) {
            return Optional.empty();
        }

        List<Long> excludedIds = excluded.isEmpty() ? List.of() : excluded.stream().toList();
        int excludeCount = excluded.size();

        List<PartnerCandidateView> playedRaw = partnerPickRepository.findFormerPartners(
                userId,
                userRating,
                pairLimit,
                maxPlayerLimit,
                Discipline.D,
                allowedSexes,
                excludeCount,
                excludedIds);

        List<PartnerCandidateView> newcomerRaw = partnerPickRepository.findCandidates(
                userId,
                userRating,
                pairLimit,
                maxPlayerLimit,
                Discipline.D,
                allowedSexes,
                excludeCount,
                excludedIds);
        if (newcomerRaw.size() > MAX_CANDIDATES) {
            newcomerRaw = newcomerRaw.subList(0, MAX_CANDIDATES);
        }

        Set<Long> formerPartnerIds = playedRaw.stream()
                .map(PartnerCandidateView::getPlayerId)
                .collect(Collectors.toCollection(HashSet::new));
        newcomerRaw = newcomerRaw.stream()
                .filter(c -> !formerPartnerIds.contains(c.getPlayerId()))
                .toList();

        Set<Long> jointStatsIds = new HashSet<>(formerPartnerIds);
        newcomerRaw.stream().map(PartnerCandidateView::getPlayerId).forEach(jointStatsIds::add);
        Map<Long, JointStats> jointStats = loadJointStats(userId, List.copyOf(jointStatsIds));

        PairCompositionType tournamentComposition =
                TournamentDisciplineSupport.compositionForTournament(pairDiscipline);
        Instant historySince = Instant.now().minus(
                metricsProperties.partnerHistoryMonths() * 30L, ChronoUnit.DAYS);

        List<ScoredCandidate> playedScored = scoreCandidates(
                playedRaw, user, userRating, pairLimit, maxPlayerLimit,
                tournamentComposition, historySince, jointStats, true);
        List<ScoredCandidate> newcomerScored = scoreCandidates(
                newcomerRaw, user, userRating, pairLimit, maxPlayerLimit,
                tournamentComposition, historySince, jointStats, false);

        Comparator<ScoredCandidate> newcomerCmp = Comparator
                .comparing(ScoredCandidate::categoryMatch).reversed()
                .thenComparing(ScoredCandidate::score).reversed()
                .thenComparing(c -> c.candidate().getNick());

        Comparator<ScoredCandidate> playedCmp = Comparator
                .comparing(ScoredCandidate::successfulHistory).reversed()
                .thenComparing(ScoredCandidate::categoryMatch).reversed()
                .thenComparing(ScoredCandidate::score).reversed()
                .thenComparing(c -> c.candidate().getNick());

        List<PartnerCandidateRow> playedRows = playedScored.stream()
                .sorted(playedCmp)
                .limit(MAX_DISPLAY)
                .map(s -> toRow(s, userRating))
                .toList();

        List<PartnerCandidateRow> newcomerRows = newcomerScored.stream()
                .sorted(newcomerCmp)
                .limit(MAX_DISPLAY)
                .map(s -> toRow(s, userRating))
                .toList();

        String userFullName = Names.fullName(user.getLastName(), user.getFirstName(), user.getPatronymic());
        return Optional.of(new PartnerPickPage(
                tournamentId,
                tournament.getName(),
                tournament.getStartsAt(),
                tournament.getRatingLimit(),
                tournament.getMaxPlayerRatingLimit(),
                userId,
                userFullName,
                user.getNick(),
                userRating,
                playedRows,
                newcomerRows));
    }

    private List<ScoredCandidate> scoreCandidates(List<PartnerCandidateView> raw,
                                                  Player user,
                                                  double userRating,
                                                  Double pairLimit,
                                                  Double maxPlayerLimit,
                                                  PairCompositionType tournamentComposition,
                                                  Instant historySince,
                                                  Map<Long, JointStats> jointStats,
                                                  boolean playedBeforeBlock) {
        List<ScoredCandidate> scored = new ArrayList<>();
        for (PartnerCandidateView c : raw) {
            double candidateRating = c.getRating().doubleValue();
            JointStats joint = jointStats.getOrDefault(c.getPlayerId(), JointStats.EMPTY);
            boolean successful = joint.positiveDeltaSince(historySince);
            boolean playedBefore = playedBeforeBlock || joint.hasMeetings();
            double playability = playabilityIndexService.index(joint.meetingTimes());
            double weightedJointDelta = playabilityIndexService.weightedValueSum(
                    Instant.now(), joint.timedJointDeltas());
            OptionalDouble candidateForm = playerFormService.formIfKnown(c.getPlayerId());
            Optional<GameAccentResult> accent = playerGameAccentService.accentForCard(c.getPlayerId());
            double tournamentCategoryDelta = 0;
            if (tournamentComposition != null) {
                OptionalDouble categoryAvg = playerGameAccentService
                        .avgDeltaForComposition(c.getPlayerId(), tournamentComposition);
                if (categoryAvg.isPresent() && categoryAvg.getAsDouble() > 0) {
                    tournamentCategoryDelta = categoryAvg.getAsDouble();
                }
            }

            var scoreResult = partnerScoreService.score(new PartnerScoreService.Input(
                    userRating,
                    candidateRating,
                    pairLimit,
                    maxPlayerLimit,
                    weightedJointDelta,
                    playability,
                    candidateForm,
                    tournamentCategoryDelta,
                    playerStabilityService.stabilityLevelIfKnown(c.getPlayerId())));

            boolean categoryMatch = accent.isPresent()
                    && tournamentComposition != null
                    && tournamentComposition == accent.get().preferenceType();
            boolean goodForm = candidateForm.isPresent() && candidateForm.getAsDouble() > 0;
            PairCompositionType futurePair = pairCompositionService.resolve(user.getSex(), c.getSex());
            boolean ideal = successful && categoryMatch && goodForm;

            scored.add(new ScoredCandidate(
                    c,
                    candidateRating,
                    scoreResult.score(),
                    successful,
                    playedBefore,
                    categoryMatch,
                    goodForm,
                    ideal,
                    futurePair));
        }
        return scored;
    }

    private static boolean fitsTournamentRating(double userRating, Double maxPlayerLimit) {
        if (maxPlayerLimit != null && maxPlayerLimit > 0 && userRating > maxPlayerLimit) {
            return false;
        }
        return true;
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
            boolean playedBefore,
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

        boolean hasMeetings() {
            return !meetingTimes.isEmpty();
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

        List<TimedValue> timedJointDeltas() {
            return deltas.stream().map(d -> new TimedValue(d.at, d.sum)).toList();
        }

        private record DeltaAt(Instant at, double sum) {}
    }
}
