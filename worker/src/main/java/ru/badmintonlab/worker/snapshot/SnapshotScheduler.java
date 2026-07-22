package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ежедневный запуск слепка по расписанию. Включается свойством
 * {@code badminton-lab.snapshot.scheduled-enabled=true}; время — {@code badminton-lab.snapshot.cron}.
 */
@Component
@ConditionalOnProperty(prefix = "badminton-lab.snapshot", name = "scheduled-enabled", havingValue = "true")
public class SnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final SnapshotService snapshotService;

    public SnapshotScheduler(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Scheduled(cron = "${badminton-lab.snapshot.cron:0 0 4 * * *}", zone = "Europe/Moscow")
    public void scheduledSnapshot() {
        log.info("Запуск слепка по расписанию");
        snapshotService.runSnapshot();
    }
}
