package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.TournamentDisciplineSupport;
import ru.badmintonlab.parser.model.TournamentRatingLimits;
import ru.badmintonlab.parser.support.ParseUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Лимиты из {@code section.tour-desc}: «огр. по рейтингу …» и «макс. рейтинг одного игрока в паре».
 */
public class TournamentRatingLimitsParser {

    private static final Pattern MAX_PLAYER = Pattern.compile(
            "\\((\\d+(?:-\\d+)?)\\s*-\\s*макс\\.\\s*рейтинг\\s+одного\\s+игрока\\s+в\\s+паре\\)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public TournamentRatingLimits parse(Document document, String categoryCode, String tournamentName) {
        Element section = document.selectFirst("section.tour-desc");
        if (section == null) {
            return TournamentRatingLimits.noLimit();
        }
        List<Block> blocks = parseBlocks(section);
        if (blocks.isEmpty()) {
            return TournamentRatingLimits.noLimit();
        }
        Discipline pairDiscipline = TournamentDisciplineSupport.pairDiscipline(categoryCode, tournamentName);
        Block chosen = chooseBlock(blocks, pairDiscipline);
        BigDecimal pairLimit = chosen.pairLimit();
        BigDecimal maxPlayer = chosen.parsedMaxPlayer().orElse(pairLimit);
        return new TournamentRatingLimits(Optional.of(pairLimit), Optional.of(maxPlayer));
    }

    private List<Block> parseBlocks(Element section) {
        List<Block> blocks = new ArrayList<>();
        for (Element p : section.select("p")) {
            String text = p.text();
            if (text == null || !text.toLowerCase(Locale.ROOT).contains("огр. по рейтингу")) {
                continue;
            }
            Element var = p.selectFirst("var");
            if (var == null) {
                continue;
            }
            Optional<BigDecimal> pair = ParseUtils.parseRatingLimitVar(var.text());
            if (pair.isEmpty()) {
                continue;
            }
            Optional<BigDecimal> maxPlayer = parseMaxPlayer(p.text());
            Discipline kind = disciplineFromText(text);
            blocks.add(new Block(kind, pair.get(), maxPlayer));
        }
        return blocks;
    }

    private static Optional<BigDecimal> parseMaxPlayer(String paragraphText) {
        Matcher matcher = MAX_PLAYER.matcher(paragraphText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return ParseUtils.parseRatingLimitVar(matcher.group(1));
    }

    private static Discipline disciplineFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("мужск")) {
            return Discipline.MD;
        }
        if (lower.contains("женск")) {
            return Discipline.WD;
        }
        if (lower.contains("микст")) {
            return Discipline.XD;
        }
        return Discipline.D;
    }

    private static Block chooseBlock(List<Block> blocks, Discipline pairDiscipline) {
        if (blocks.size() == 1) {
            return blocks.get(0);
        }
        return switch (pairDiscipline) {
            case MD -> findByKind(blocks, Discipline.MD).orElse(blocks.get(0));
            case WD -> findByKind(blocks, Discipline.WD).orElse(blocks.get(0));
            case XD -> findByKind(blocks, Discipline.XD).orElse(blocks.get(0));
            default -> blocks.stream()
                    .min(Comparator.comparing(Block::pairLimit))
                    .orElse(blocks.get(0));
        };
    }

    private static Optional<Block> findByKind(List<Block> blocks, Discipline kind) {
        return blocks.stream().filter(b -> b.kind() == kind).findFirst();
    }

    private record Block(Discipline kind, BigDecimal pairLimit, Optional<BigDecimal> parsedMaxPlayer) {
    }
}
