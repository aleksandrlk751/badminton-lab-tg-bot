package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.PlayerSex;
import ru.badmintonlab.parser.PlayerProfileSexEvidenceParser;
import ru.badmintonlab.parser.model.PlayerProfileSexEvidence;
import ru.badmintonlab.worker.http.Badminton4uClient;

/**
 * Fallback пола по участиям из профиля игрока на сайте (все регионы, без {@code cities[]}).
 */
@Service
public class PlayerSexProfileFallbackService {

    private static final Logger log = LoggerFactory.getLogger(PlayerSexProfileFallbackService.class);

    private final Badminton4uClient client;
    private final PlayerSexUpsertService sexUpsertService;
    private final PlayerProfileSexEvidenceParser evidenceParser = new PlayerProfileSexEvidenceParser();

    public PlayerSexProfileFallbackService(Badminton4uClient client,
                                           PlayerSexUpsertService sexUpsertService) {
        this.client = client;
        this.sexUpsertService = sexUpsertService;
    }

    /**
     * @return число игроков, у которых пол выведен из профиля
     */
    public int inferMissingFromProfiles() {
        int updated = 0;
        int failed = 0;
        for (long playerId : sexUpsertService.findPlayerIdsWithMissingSex()) {
            try {
                PlayerProfileSexEvidence evidence = evidenceParser.parse(client.playerProfile(playerId));
                PlayerSex inferred = sexUpsertService.inferFromProfileEvidence(evidence);
                if (inferred != null && sexUpsertService.applyInferredSex(playerId, inferred)) {
                    updated++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("Профиль {}: fallback пола не удался: {}", playerId, e.toString());
            }
        }
        if (failed > 0) {
            log.info("Fallback пола из профилей: ошибок загрузки/разбора={}", failed);
        }
        return updated;
    }
}
