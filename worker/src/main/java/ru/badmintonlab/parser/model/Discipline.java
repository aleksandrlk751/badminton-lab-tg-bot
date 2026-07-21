package ru.badmintonlab.parser.model;

import java.util.Optional;

public enum Discipline {
    S, D, MS, WS, MD, WD, XD;

    public static Optional<Discipline> fromSiteCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return switch (code.toLowerCase()) {
            case "s" -> Optional.of(S);
            case "d" -> Optional.of(D);
            case "ms" -> Optional.of(MS);
            case "ws" -> Optional.of(WS);
            case "md" -> Optional.of(MD);
            case "wd" -> Optional.of(WD);
            case "xd" -> Optional.of(XD);
            default -> Optional.empty();
        };
    }

    public static Optional<Discipline> fromRatingTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return Optional.empty();
        }
        return switch (tab) {
            case "rat_s" -> Optional.of(S);
            case "rat_d" -> Optional.of(D);
            case "rat_ms" -> Optional.of(MS);
            case "rat_ws" -> Optional.of(WS);
            case "rat_md" -> Optional.of(MD);
            case "rat_wd" -> Optional.of(WD);
            case "rat_xd" -> Optional.of(XD);
            default -> Optional.empty();
        };
    }
}
