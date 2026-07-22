package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ручной dev-триггер: выполняет слепок один раз при старте, если задано
 * {@code badminton-lab.snapshot.run-on-startup=true}. Слепок выполняется синхронно —
 * это одноразовый сценарий разработки; регулярный запуск — через {@link SnapshotScheduler}.
 */
@Component
@Order(0)
@ConditionalOnProperty(prefix = "badminton-lab.snapshot", name = "run-on-startup", havingValue = "true")
public class SnapshotStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SnapshotStartupRunner.class);

    private final SnapshotService snapshotService;

    public SnapshotStartupRunner(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("run-on-startup=true — выполняю слепок синхронно");
        try {
            snapshotService.runSnapshot();
        } catch (RuntimeException e) {
            log.error("Стартовый слепок завершился ошибкой: {}", e.toString(), e);
        }
    }
}
