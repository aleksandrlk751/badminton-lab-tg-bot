package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Офлайн-дозаполнение пола по ФИО из локальной БД (без справочника и профилей на сайте).
 * {@code badminton-lab.snapshot.infer-sex-from-names-on-startup=true}
 */
@Component
@Order(0)
@ConditionalOnProperty(prefix = "badminton-lab.snapshot", name = "infer-sex-from-names-on-startup", havingValue = "true")
public class NameSexStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NameSexStartupRunner.class);

    private final PlayerSexUpsertService sexUpsertService;

    public NameSexStartupRunner(PlayerSexUpsertService sexUpsertService) {
        this.sexUpsertService = sexUpsertService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("infer-sex-from-names-on-startup=true — офлайн fallback по ФИО");
            int updated = sexUpsertService.inferMissingFromNames();
            log.info("Fallback пола по ФИО: обновлено {} игроков — завершаю процесс", updated);
        } catch (RuntimeException e) {
            log.error("Fallback пола по ФИО завершился ошибкой: {}", e.toString(), e);
        }
        System.exit(0);
    }
}
