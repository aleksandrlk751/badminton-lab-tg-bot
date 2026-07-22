package ru.badmintonlab.worker.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.TournamentStatus;
import ru.badmintonlab.core.entity.Tournament;
import ru.badmintonlab.core.repository.TournamentRepository;
import ru.badmintonlab.parser.model.TournamentListEntry;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Идемпотентный upsert турнира. Поля берутся из строки списка «призёров»
 * (id, дата/время, лимит, название); дисциплина известна из типа запроса.
 */
@Service
public class TournamentUpsertService {

    private final TournamentRepository tournamentRepository;

    public TournamentUpsertService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional
    public void upsert(TournamentListEntry entry, String regionCode, TournamentStatus status) {
        Tournament tournament = tournamentRepository.findById(entry.id())
                .orElseGet(() -> new Tournament(entry.id()));
        tournament.setName(entry.name());
        tournament.setCategoryCode(extractCategory(entry.name()));
        tournament.setRatingLimit(entry.ratingLimit().orElse(null));
        tournament.setRegionCode(regionCode);
        tournament.setStatus(status);

        LocalTime time = entry.time().orElse(LocalTime.MIDNIGHT);
        tournament.setStartsAt(SnapshotSupport.toInstant(LocalDateTime.of(entry.date(), time)));

        tournamentRepository.save(tournament);
    }

    private String extractCategory(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lastToken = name.trim().substring(name.trim().lastIndexOf(' ') + 1);
        if (lastToken.length() > 16) {
            return null;
        }
        return lastToken;
    }
}
