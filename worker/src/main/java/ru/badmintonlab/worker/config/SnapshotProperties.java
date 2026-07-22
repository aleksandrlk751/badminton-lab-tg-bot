package ru.badmintonlab.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.badmintonlab.core.domain.Discipline;

import java.util.List;

/**
 * Параметры слепка региона: код региона, глубина в годах, набор дисциплин,
 * ручной запуск на старте и ограничение числа турниров (для «дымового» прогона).
 */
@ConfigurationProperties(prefix = "badminton-lab.snapshot")
public record SnapshotProperties(
        String regionCode,
        int yearsBack,
        List<Discipline> disciplines,
        boolean runOnStartup,
        int maxTournaments,
        boolean scheduledEnabled
) {
    public SnapshotProperties {
        if (regionCode == null || regionCode.isBlank()) {
            regionCode = "r77";
        }
        if (yearsBack <= 0) {
            yearsBack = 3;
        }
        if (disciplines == null || disciplines.isEmpty()) {
            disciplines = List.of(Discipline.D, Discipline.MD, Discipline.WD, Discipline.XD);
        }
        // maxTournaments <= 0 трактуем как «без лимита» (полный слепок).
    }

    public boolean isUnlimited() {
        return maxTournaments <= 0;
    }
}
