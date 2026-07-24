package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.badmintonlab.worker.config.SnapshotProperties;

/**
 * Разовое проставление лимитов рейтинга по страницам турниров.
 * {@code badminton-lab.snapshot.backfill-rating-limits-on-startup=true}
 */
@Component
@Order(0)
@ConditionalOnProperty(prefix = "badminton-lab.snapshot", name = "backfill-rating-limits-on-startup", havingValue = "true")
public class TournamentRatingLimitsBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TournamentRatingLimitsBackfillRunner.class);

    private final TournamentRatingLimitsBackfillService backfillService;
    private final SnapshotProperties snapshotProperties;

    public TournamentRatingLimitsBackfillRunner(TournamentRatingLimitsBackfillService backfillService,
                                                SnapshotProperties snapshotProperties) {
        this.backfillService = backfillService;
        this.snapshotProperties = snapshotProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("backfill-rating-limits-on-startup=true — регион {}", snapshotProperties.regionCode());
            backfillService.backfillAllInRegion(snapshotProperties.regionCode());
        } catch (RuntimeException e) {
            log.error("Backfill лимитов рейтинга завершился ошибкой: {}", e.toString(), e);
        }
        System.exit(0);
    }
}
