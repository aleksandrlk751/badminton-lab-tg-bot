package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.TournamentRegistration;
import ru.badmintonlab.parser.support.ParseUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсит страницу будущего турнира: заявки и формы «ищу пару».
 * Список пар/игроков на сайте часто подгружается AJAX в {@code #tour-reg-list1/2} —
 * в fixture может быть только пустой placeholder.
 */
public class TournamentRegistrationParser {

    public TournamentRegistration parse(Document document) {
        long tournamentId = extractTournamentId(document);
        boolean seekingPartnerFormPresent = document.selectFirst("#tour-reg-form2") != null;

        List<TournamentRegistration.RegisteredPlayer> players = new ArrayList<>();
        players.addAll(parseRegistrationSection(document, "tour-reg-list2", true, seekingPartnerFormPresent));
        players.addAll(parseRegistrationSection(document, "tour-reg-list1", false, seekingPartnerFormPresent));

        if (players.isEmpty()) {
            players.addAll(parseInlineRegisteredPlayers(document, seekingPartnerFormPresent));
        }

        List<TournamentRegistration.RegisteredPair> pairs = parsePairs(document);

        return new TournamentRegistration(tournamentId, List.copyOf(dedupePlayers(players).values()), pairs);
    }

    private Map<Long, TournamentRegistration.RegisteredPlayer> dedupePlayers(
            List<TournamentRegistration.RegisteredPlayer> players
    ) {
        Map<Long, TournamentRegistration.RegisteredPlayer> byId = new LinkedHashMap<>();
        for (TournamentRegistration.RegisteredPlayer player : players) {
            byId.merge(player.playerId(), player, (existing, incoming) ->
                    new TournamentRegistration.RegisteredPlayer(
                            existing.playerId(),
                            existing.nick(),
                            existing.seekingPartner() || incoming.seekingPartner()
                    ));
        }
        return byId;
    }

    private List<TournamentRegistration.RegisteredPlayer> parseRegistrationSection(
            Document document,
            String sectionId,
            boolean seekingPartnerSection,
            boolean seekingPartnerFormPresent
    ) {
        Element section = document.getElementById(sectionId);
        if (section == null || section.children().isEmpty()) {
            return List.of();
        }

        List<TournamentRegistration.RegisteredPlayer> players = new ArrayList<>();
        for (Element link : section.select("a[href*=players/]")) {
            long playerId = ParseUtils.extractPlayerId(link.attr("href")).orElseThrow();
            boolean seeking = seekingPartnerSection && seekingPartnerFormPresent;
            players.add(new TournamentRegistration.RegisteredPlayer(playerId, link.text().trim(), seeking));
        }
        return players;
    }

    private List<TournamentRegistration.RegisteredPlayer> parseInlineRegisteredPlayers(
            Document document,
            boolean seekingPartnerFormPresent
    ) {
        Element tourDesc = document.selectFirst("section.tour-desc");
        if (tourDesc == null) {
            return List.of();
        }

        List<TournamentRegistration.RegisteredPlayer> players = new ArrayList<>();
        for (Element link : tourDesc.select("a[href*=players/]")) {
            long playerId = ParseUtils.extractPlayerId(link.attr("href")).orElseThrow();
            players.add(new TournamentRegistration.RegisteredPlayer(
                    playerId,
                    link.text().trim(),
                    seekingPartnerFormPresent
            ));
        }
        return players;
    }

    private List<TournamentRegistration.RegisteredPair> parsePairs(Document document) {
        List<TournamentRegistration.RegisteredPair> pairs = new ArrayList<>();

        for (String sectionId : List.of("tour-reg-list1", "tour-reg-list")) {
            Element section = document.getElementById(sectionId);
            if (section == null) {
                continue;
            }
            for (Element row : section.select("tr")) {
                Elements links = row.select("a[href*=players/]");
                if (links.size() >= 2) {
                    pairs.add(toPair(links));
                }
            }
            for (Element block : section.select("p, div, li")) {
                Elements links = block.select("a[href*=players/]");
                if (links.size() >= 2) {
                    pairs.add(toPair(links));
                }
            }
        }

        Element regTable = document.selectFirst("table.tour-reg, table.tour-doubles.tour-reg");
        if (regTable != null) {
            for (Element row : regTable.select("tr")) {
                Elements links = row.select("a[href*=players/]");
                if (links.size() >= 2) {
                    pairs.add(toPair(links));
                }
            }
        }
        return pairs;
    }

    private TournamentRegistration.RegisteredPair toPair(Elements links) {
        long p1 = ParseUtils.extractPlayerId(links.get(0).attr("href")).orElseThrow();
        long p2 = ParseUtils.extractPlayerId(links.get(1).attr("href")).orElseThrow();
        return new TournamentRegistration.RegisteredPair(
                p1,
                p2,
                Optional.of(links.get(0).text().trim()),
                Optional.of(links.get(1).text().trim())
        );
    }

    private long extractTournamentId(Document document) {
        Element commentButton = document.selectFirst("button[onclick^=addComment]");
        if (commentButton != null) {
            Matcher matcher = Pattern.compile("addComment\\((\\d+)").matcher(commentButton.attr("onclick"));
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        Element regButton = document.selectFirst("[onclick*=regToTourDouble], [onclick*=regToTourTeamUser]");
        if (regButton != null) {
            Matcher matcher = Pattern.compile("regToTour(?:Double|TeamUser)\\((\\d+)").matcher(regButton.attr("onclick"));
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        Element breadcrumb = document.selectFirst("ol.breadcrumbs a[href*=tournaments/]");
        if (breadcrumb != null) {
            return ParseUtils.extractTournamentId(breadcrumb.attr("href")).orElseThrow();
        }
        throw new IllegalStateException("Tournament id not found on registration page");
    }
}
