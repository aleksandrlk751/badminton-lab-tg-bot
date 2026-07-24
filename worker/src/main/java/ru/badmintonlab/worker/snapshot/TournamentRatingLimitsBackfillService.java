package ru.badmintonlab.worker.snapshot;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.entity.Tournament;
import ru.badmintonlab.core.repository.TournamentRepository;
import ru.badmintonlab.parser.TournamentRatingLimitsParser;
import ru.badmintonlab.parser.model.TournamentRatingLimits;
import ru.badmintonlab.worker.http.Badminton4uClient;

import java.util.List;

/**
 * Проставляет {@code rating_limit} и {@code max_player_rating_limit} по HTML страницы турнира.
 */
@Service
public class TournamentRatingLimitsBackfillService {

    private static final Logger log = LoggerFactory.getLogger(TournamentRatingLimitsBackfillService.class);

    private final Badminton4uClient client;
    private final TournamentRepository tournamentRepository;
    private final TournamentRatingLimitsParser limitsParser = new TournamentRatingLimitsParser();

    public TournamentRatingLimitsBackfillService(Badminton4uClient client,
                                                 TournamentRepository tournamentRepository) {
        this.client = client;
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional
    public void applyFromPage(Tournament tournament, Document page) {
        TournamentRatingLimits limits = limitsParser.parse(
                page, tournament.getCategoryCode(), tournament.getName());
        applyLimits(tournament, limits);
    }

    static void applyLimits(Tournament tournament, TournamentRatingLimits limits) {
        if (limits.pairRatingLimit().isEmpty()) {
            tournament.setRatingLimit(null);
            tournament.setMaxPlayerRatingLimit(null);
            return;
        }
        tournament.setRatingLimit(limits.pairRatingLimit().orElse(null));
        tournament.setMaxPlayerRatingLimit(limits.maxPlayerRatingLimit().orElse(null));
    }

    public int backfillAllInRegion(String regionCode) {
        List<Tournament> tournaments = tournamentRepository.findAll().stream()
                .filter(t -> regionCode.equals(t.getRegionCode()))
                .toList();
        int updated = 0;
        for (Tournament tournament : tournaments) {
            try {
                Document page = client.tournamentPage(tournament.getId());
                applyFromPage(tournament, page);
                tournamentRepository.save(tournament);
                updated++;
                if (updated % 50 == 0) {
                    log.info("Лимиты рейтинга: обработано {} / {}", updated, tournaments.size());
                }
            } catch (RuntimeException e) {
                log.warn("Лимиты рейтинга: турнир {} — {}", tournament.getId(), e.toString());
            }
        }
        log.info("Лимиты рейтинга: обновлено {} турниров региона {}", updated, regionCode);
        return updated;
    }
}
