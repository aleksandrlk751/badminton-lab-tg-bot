package ru.badmintonlab.parser.model;

import java.util.List;
import java.util.Optional;

public record TournamentRegistration(
        long tournamentId,
        List<RegisteredPlayer> players,
        List<RegisteredPair> pairs
) {
    public record RegisteredPlayer(long playerId, String nick, boolean seekingPartner) {}

    public record RegisteredPair(long player1Id, long player2Id, Optional<String> player1Nick, Optional<String> player2Nick) {}
}
