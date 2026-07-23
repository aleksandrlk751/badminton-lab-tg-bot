package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.PlayerSex;
import ru.badmintonlab.worker.http.PlayerDirectoryListType;
import ru.badmintonlab.worker.http.PlayerDirectoryLoader;

/**
 * Синхронизация пола: справочник (singles + doubles), локальный fallback, профиль игрока (все регионы).
 */
@Service
public class PlayerSexSyncService {

    private static final Logger log = LoggerFactory.getLogger(PlayerSexSyncService.class);

    private final PlayerDirectoryLoader directoryLoader;
    private final PlayerSexUpsertService sexUpsertService;
    private final PlayerSexProfileFallbackService profileFallbackService;

    public PlayerSexSyncService(PlayerDirectoryLoader directoryLoader,
                                PlayerSexUpsertService sexUpsertService,
                                PlayerSexProfileFallbackService profileFallbackService) {
        this.directoryLoader = directoryLoader;
        this.sexUpsertService = sexUpsertService;
        this.profileFallbackService = profileFallbackService;
    }

    /**
     * @return метрики шага синхронизации пола
     */
    public PlayerSexSyncMetrics syncRegion(String regionCode) {
        int malesUpdated = upsertDirectory(regionCode, PlayerSex.M);
        int femalesUpdated = upsertDirectory(regionCode, PlayerSex.F);
        int inferredLocal = sexUpsertService.inferMissingFromLocalDisciplines();
        int inferredProfile = profileFallbackService.inferMissingFromProfiles();
        int inferredNames = sexUpsertService.inferMissingFromNames();

        PlayerSexSyncMetrics metrics = new PlayerSexSyncMetrics(
                malesUpdated, femalesUpdated, inferredLocal, inferredProfile, inferredNames);
        log.info("Синхронизация пола региона {}: {}", regionCode, metrics);
        return metrics;
    }

    private int upsertDirectory(String regionCode, PlayerSex sex) {
        int updated = 0;
        for (PlayerDirectoryListType listType : PlayerDirectoryListType.values()) {
            updated += sexUpsertService.upsertFromDirectory(
                    directoryLoader.loadAll(regionCode, sex, listType), sex);
        }
        return updated;
    }

    public record PlayerSexSyncMetrics(
            int malesUpdated,
            int femalesUpdated,
            int inferredFromLocalDisciplines,
            int inferredFromProfileParticipations,
            int inferredFromNames) {

        @Override
        public String toString() {
            return "мужчин обновлено=" + malesUpdated
                    + ", женщин обновлено=" + femalesUpdated
                    + ", выведено из слепка=" + inferredFromLocalDisciplines
                    + ", выведено из профилей=" + inferredFromProfileParticipations
                    + ", выведено из ФИО=" + inferredFromNames;
        }
    }
}
