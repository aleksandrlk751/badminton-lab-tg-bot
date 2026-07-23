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
 * Разовая синхронизация пола из справочника без полного слепка.
 * {@code badminton-lab.snapshot.sync-sex-on-startup=true}
 */
@Component
@Order(0)
@ConditionalOnProperty(prefix = "badminton-lab.snapshot", name = "sync-sex-on-startup", havingValue = "true")
public class SexSyncStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SexSyncStartupRunner.class);

    private final PlayerSexSyncService playerSexSyncService;
    private final SnapshotProperties snapshotProperties;

    public SexSyncStartupRunner(PlayerSexSyncService playerSexSyncService,
                                  SnapshotProperties snapshotProperties) {
        this.playerSexSyncService = playerSexSyncService;
        this.snapshotProperties = snapshotProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("sync-sex-on-startup=true — синхронизация пола региона {}", snapshotProperties.regionCode());
            playerSexSyncService.syncRegion(snapshotProperties.regionCode());
            log.info("Синхронизация пола завершена — завершаю процесс");
        } catch (RuntimeException e) {
            log.error("Синхронизация пола завершилась ошибкой: {}", e.toString(), e);
        }
        System.exit(0);
    }
}
