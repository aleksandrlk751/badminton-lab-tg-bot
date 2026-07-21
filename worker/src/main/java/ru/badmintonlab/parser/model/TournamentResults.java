package ru.badmintonlab.parser.model;

import java.util.List;

public record TournamentResults(
        long tournamentId,
        List<TournamentPairResult> pairs
) {
}
