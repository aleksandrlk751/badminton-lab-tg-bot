package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.bot.util.TournamentLinks;
import ru.badmintonlab.core.entity.Tournament;
import ru.badmintonlab.core.repository.TournamentRepository;
import ru.badmintonlab.worker.snapshot.UpcomingTournamentsSyncService;

import java.time.Instant;
import java.util.Optional;

@Service
public class PartnerTournamentResolveService {

    private final UpcomingTournamentsSyncService upcomingSync;
    private final TournamentRepository tournamentRepository;

    public PartnerTournamentResolveService(UpcomingTournamentsSyncService upcomingSync,
                                           TournamentRepository tournamentRepository) {
        this.upcomingSync = upcomingSync;
        this.tournamentRepository = tournamentRepository;
    }

    public enum LinkError {
        INVALID_LINK,
        NOT_DOUBLES,
        NOT_FOUND,
        ALREADY_STARTED,
        SYNC_FAILED
    }

    public record ResolveResult(Optional<Long> tournamentId, Optional<LinkError> error) {
        public static ResolveResult ok(long id) {
            return new ResolveResult(Optional.of(id), Optional.empty());
        }

        public static ResolveResult fail(LinkError error) {
            return new ResolveResult(Optional.empty(), Optional.of(error));
        }
    }

    @Transactional
    public ResolveResult resolveFromLink(String text) {
        var idOpt = TournamentLinks.parseTournamentId(text);
        if (idOpt.isEmpty()) {
            return ResolveResult.fail(LinkError.INVALID_LINK);
        }
        long id = idOpt.getAsLong();
        try {
            upcomingSync.syncById(id);
        } catch (IllegalArgumentException e) {
            return ResolveResult.fail(LinkError.NOT_DOUBLES);
        } catch (RuntimeException e) {
            return ResolveResult.fail(LinkError.SYNC_FAILED);
        }
        Optional<Tournament> tournament = tournamentRepository.findById(id);
        if (tournament.isEmpty()) {
            return ResolveResult.fail(LinkError.NOT_FOUND);
        }
        if (!tournament.get().getStartsAt().isAfter(Instant.now())) {
            return ResolveResult.fail(LinkError.ALREADY_STARTED);
        }
        return ResolveResult.ok(id);
    }
}
