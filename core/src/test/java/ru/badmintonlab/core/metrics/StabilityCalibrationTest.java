package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

/**
 * Калибровка зон стабильности на локальной БД (postgres:5433).
 * Запуск: {@code mvn -pl core test -Dtest=StabilityCalibrationTest -Dgroups=calibration}.
 */
@Tag("calibration")
@Disabled("Локальная калибровка — вручную при наличии postgres")
class StabilityCalibrationTest {

    private static final String JDBC =
            "jdbc:postgresql://localhost:5433/badminton_lab?user=badminton&password=badminton";

    @Test
    void printDistributionOnLocalSnapshot() throws Exception {
        StabilityService service = new StabilityService(TestMetrics.defaults());
        List<PlayerStability> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(JDBC)) {
            List<Long> playerIds = loadActivePlayerIds(conn, 500);
            for (long playerId : playerIds) {
                List<StabilityMatchEvent> events = loadEvents(conn, playerId);
                Optional<Double> stability = service.stability(events);
                if (stability.isPresent()) {
                    rows.add(new PlayerStability(playerId, loadNick(conn, playerId), stability.get()));
                }
            }
        }

        rows.sort(Comparator.comparingDouble(PlayerStability::stability));
        DoubleSummaryStatistics stats = rows.stream()
                .mapToDouble(PlayerStability::stability)
                .summaryStatistics();

        System.out.println("=== Stability calibration (ε="
                + TestMetrics.defaults().stabilitySurpriseThreshold()
                + ", pair disciplines, n=" + rows.size() + ") ===");
        System.out.printf("min=%.1f p10=%.1f p25=%.1f median=%.1f p75=%.1f p90=%.1f max=%.1f avg=%.1f%n",
                stats.getMin(), percentile(rows, 10), percentile(rows, 25), percentile(rows, 50),
                percentile(rows, 75), percentile(rows, 90), stats.getMax(), stats.getAverage());

        printZone("🔴 very unstable", 0, 55, rows);
        printZone("🟡 unstable", 55, 70, rows);
        printZone("⚪ middle", 70, 82, rows);
        printZone("🟢 stable", 82, 92, rows);
        printZone("💚 super stable", 92, 101, rows);
    }

    @Test
    void printWideDistribution() throws Exception {
        StabilityService service = new StabilityService(TestMetrics.defaults());
        List<PlayerStability> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(JDBC)) {
            String sql = """
                    SELECT mp.player_id
                    FROM match_player mp
                    JOIN match m ON m.id = mp.match_id
                    WHERE mp.rating_delta IS NOT NULL
                      AND m.discipline IN ('D', 'MD', 'WD', 'XD')
                    GROUP BY mp.player_id
                    HAVING COUNT(DISTINCT m.tournament_id) >= 5
                    """;
            List<Long> playerIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    playerIds.add(rs.getLong(1));
                }
            }
            for (long playerId : playerIds) {
                List<StabilityMatchEvent> events = loadEvents(conn, playerId);
                Optional<Double> stability = service.stability(events);
                if (stability.isPresent()) {
                    rows.add(new PlayerStability(playerId, loadNick(conn, playerId), stability.get()));
                }
            }
        }

        rows.sort(Comparator.comparingDouble(PlayerStability::stability));
        DoubleSummaryStatistics stats = rows.stream()
                .mapToDouble(PlayerStability::stability)
                .summaryStatistics();

        System.out.println("=== Wide stability calibration (≥5 tournaments, n=" + rows.size() + ") ===");
        System.out.printf("min=%.1f p5=%.1f p10=%.1f p25=%.1f median=%.1f p75=%.1f p90=%.1f p95=%.1f max=%.1f avg=%.1f%n",
                stats.getMin(), percentile(rows, 5), percentile(rows, 10), percentile(rows, 25),
                percentile(rows, 50), percentile(rows, 75), percentile(rows, 90), percentile(rows, 95),
                stats.getMax(), stats.getAverage());
        rows.stream().limit(10).forEach(r ->
                System.out.printf("  low: %6.1f %s%n", r.stability(), r.nick()));
        rows.stream().skip(Math.max(0, rows.size() - 10)).forEach(r ->
                System.out.printf("  high: %6.1f %s%n", r.stability(), r.nick()));
    }

    @Test
    void printQuintileBoundaries() throws Exception {
        StabilityService service = new StabilityService(TestMetrics.defaults());
        for (int minT : new int[] {5, 15}) {
            List<PlayerStability> rows = loadAllPlayerIds(service, minT);
            rows.sort(Comparator.comparingDouble(PlayerStability::stability));
            int n = rows.size();
            System.out.println();
            System.out.println("=== Quintiles ε="
                    + TestMetrics.defaults().stabilitySurpriseThreshold()
                    + ", ≥" + minT + " tournaments, n=" + n + " ===");
            for (int p : new int[] {5, 10, 20, 25, 33, 40, 50, 60, 67, 75, 80, 90, 95}) {
                System.out.printf("  p%d = %.1f%n", p, percentile(rows, p));
            }
            double z2 = percentile(rows, 20);
            double z3 = percentile(rows, 40);
            double z4 = percentile(rows, 60);
            double z5 = percentile(rows, 80);
            System.out.printf("  equal-quintile cutoffs: %.1f / %.1f / %.1f / %.1f%n", z2, z3, z4, z5);
            simulateZones("exact quintiles", rows, z2, z3, z4, z5);
            simulateZones("rounded quintiles", rows, roundBound(z2), roundBound(z3), roundBound(z4), roundBound(z5));
            simulateZones("85/92/96/98", rows, 85, 92, 96, 98);
            simulateZones("80/88/93/97", rows, 80, 88, 93, 97);
            simulateZones("75/85/92/96", rows, 75, 85, 92, 96);
            simulateZones("current 70/80/86/92", rows, 70, 80, 86, 92);
        }
    }

    private static double roundBound(double value) {
        return Math.round(value);
    }

    private static void simulateZones(String label, List<PlayerStability> rows,
                                      double z2, double z3, double z4, double z5) {
        System.out.printf("  %s [%s):", label, formatBound(z2));
        printZonePctInline(rows, Double.NEGATIVE_INFINITY, z2);
        System.out.printf(" [%s,%s):", formatBound(z2), formatBound(z3));
        printZonePctInline(rows, z2, z3);
        System.out.printf(" [%s,%s):", formatBound(z3), formatBound(z4));
        printZonePctInline(rows, z3, z4);
        System.out.printf(" [%s,%s):", formatBound(z4), formatBound(z5));
        printZonePctInline(rows, z4, z5);
        System.out.printf(" [%s,+∞):", formatBound(z5));
        printZonePctInline(rows, z5, Double.POSITIVE_INFINITY);
        System.out.println();
    }

    private static void printZonePctInline(List<PlayerStability> rows, double from, double to) {
        long count = rows.stream()
                .filter(r -> r.stability() >= from && r.stability() < to)
                .count();
        double pct = rows.isEmpty() ? 0.0 : 100.0 * count / rows.size();
        System.out.printf(" %.1f%%", pct);
    }

    @Test
    void printNeutralPenaltySweep() throws Exception {
        for (double neutral : new double[] {0.8, 0.85}) {
            StabilityService service = new StabilityService(TestMetrics.defaults());
            System.out.println();
            System.out.println("========== neutral S^between = " + neutral + ", ε="
                    + TestMetrics.defaults().stabilitySurpriseThreshold() + " ==========");
            printSummaryTable("≥5 tournaments", loadAllPlayerIds(service, 5, neutral));
            printSummaryTable("≥15 tournaments", loadAllPlayerIds(service, 15, neutral));
        }
    }

    private static void printSummaryTable(String label, List<PlayerStability> rows) {
        rows.sort(Comparator.comparingDouble(PlayerStability::stability));
        int n = rows.size();
        DoubleSummaryStatistics stats = rows.stream()
                .mapToDouble(PlayerStability::stability)
                .summaryStatistics();
        System.out.println();
        System.out.println("--- " + label + ", n=" + n + " ---");
        System.out.printf("min=%.1f p10=%.1f p25=%.1f median=%.1f p75=%.1f p90=%.1f max=%.1f avg=%.1f%n",
                stats.getMin(), percentile(rows, 10), percentile(rows, 25), percentile(rows, 50),
                percentile(rows, 75), percentile(rows, 90), stats.getMax(), stats.getAverage());
        printZonePct("1 🔴 <70", rows, Double.NEGATIVE_INFINITY, 70);
        printZonePct("2 🟡 70-80", rows, 70, 80);
        printZonePct("3 ⚪ 80-86", rows, 80, 86);
        printZonePct("4 🟢 86-92", rows, 86, 92);
        printZonePct("5 🔥 92+", rows, 92, Double.POSITIVE_INFINITY);
    }

    @Test
    void printProposedEmojiZones() throws Exception {
        StabilityService service = new StabilityService(TestMetrics.defaults());
        printCustomZones("≥5 tournaments", loadAllPlayerIds(service, 5));
        printCustomZones("≥15 tournaments (all)", loadAllPlayerIds(service, 15));
    }

    private List<PlayerStability> loadAllPlayerIds(StabilityService service, int minTournaments) throws Exception {
        return loadAllPlayerIds(service, minTournaments, StabilityService.NEUTRAL_BETWEEN_SCORE);
    }

    private List<PlayerStability> loadAllPlayerIds(StabilityService service, int minTournaments,
                                                   double neutralBetweenScore) throws Exception {
        List<PlayerStability> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC)) {
            String sql = """
                    SELECT mp.player_id
                    FROM match_player mp
                    JOIN match m ON m.id = mp.match_id
                    WHERE mp.rating_delta IS NOT NULL
                      AND m.discipline IN ('D', 'MD', 'WD', 'XD')
                    GROUP BY mp.player_id
                    HAVING COUNT(DISTINCT m.tournament_id) >= ?
                    """;
            List<Long> playerIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, minTournaments);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        playerIds.add(rs.getLong(1));
                    }
                }
            }
            for (long playerId : playerIds) {
                List<StabilityMatchEvent> events = loadEvents(conn, playerId);
                Optional<Double> stability = service.stability(events, neutralBetweenScore);
                if (stability.isPresent()) {
                    rows.add(new PlayerStability(playerId, loadNick(conn, playerId), stability.get()));
                }
            }
        }
        return rows;
    }

    private static void printCustomZones(String label, List<PlayerStability> rows) {
        int n = rows.size();
        System.out.println();
        System.out.println("=== Proposed zones (ε="
                + TestMetrics.defaults().stabilitySurpriseThreshold()
                + ", " + label + ", n=" + n + ") ===");
        printZonePct("1 🔴 <70", rows, Double.NEGATIVE_INFINITY, 70);
        printZonePct("2 🟡 70-80", rows, 70, 80);
        printZonePct("3 ⚪ 80-86", rows, 80, 86);
        printZonePct("4 🟢 86-92", rows, 86, 92);
        printZonePct("5 🔥 92+", rows, 92, Double.POSITIVE_INFINITY);
    }

    private static void printZonePct(String label, List<PlayerStability> rows,
                                     double fromInclusive, double toExclusive) {
        long count = rows.stream()
                .filter(r -> r.stability() >= fromInclusive && r.stability() < toExclusive)
                .count();
        double pct = rows.isEmpty() ? 0.0 : 100.0 * count / rows.size();
        String range = toExclusive == Double.POSITIVE_INFINITY
                ? "[" + formatBound(fromInclusive) + ", +∞)"
                : "[" + formatBound(fromInclusive) + ", " + formatBound(toExclusive) + ")";
        System.out.printf("  %s  %s  n=%d  %.1f%%%n", label, range, count, pct);
    }

    private static String formatBound(double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            return "-∞";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "+∞";
        }
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static List<Long> loadActivePlayerIds(Connection conn, int limit) throws Exception {
        String sql = """
                SELECT mp.player_id
                FROM match_player mp
                JOIN match m ON m.id = mp.match_id
                WHERE mp.rating_delta IS NOT NULL
                  AND m.discipline IN ('D', 'MD', 'WD', 'XD')
                GROUP BY mp.player_id
                HAVING COUNT(DISTINCT m.tournament_id) >= 15
                ORDER BY COUNT(*) DESC
                LIMIT ?
                """;
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    private static List<StabilityMatchEvent> loadEvents(Connection conn, long playerId) throws Exception {
        String sql = """
                SELECT m.tournament_id, t.starts_at, mp.rating_delta
                FROM match_player mp
                JOIN match m ON m.id = mp.match_id
                JOIN tournament t ON t.id = m.tournament_id
                WHERE mp.player_id = ?
                  AND m.discipline IN ('D', 'MD', 'WD', 'XD')
                  AND mp.rating_delta IS NOT NULL
                ORDER BY t.starts_at, m.id
                """;
        List<StabilityMatchEvent> events = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new StabilityMatchEvent(
                            rs.getLong("tournament_id"),
                            rs.getTimestamp("starts_at").toInstant(),
                            rs.getBigDecimal("rating_delta").doubleValue()));
                }
            }
        }
        return events;
    }

    private static String loadNick(Connection conn, long playerId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT nick FROM player WHERE id = ?")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : String.valueOf(playerId);
            }
        }
    }

    private static double percentile(List<PlayerStability> rows, int pct) {
        if (rows.isEmpty()) {
            return Double.NaN;
        }
        int index = Math.min(rows.size() - 1, Math.max(0, (int) Math.ceil(pct / 100.0 * rows.size()) - 1));
        return rows.get(index).stability();
    }

    private static void printZone(String label, double fromInclusive, double toExclusive, List<PlayerStability> rows) {
        List<PlayerStability> zone = rows.stream()
                .filter(r -> r.stability() >= fromInclusive && r.stability() < toExclusive)
                .toList();
        System.out.println();
        System.out.println(label + " [" + fromInclusive + ", " + toExclusive + "), n=" + zone.size());
        zone.stream().limit(5).forEach(r ->
                System.out.printf("  %6.1f  %s (%d)%n", r.stability(), r.nick(), r.playerId()));
    }

    private record PlayerStability(long playerId, String nick, double stability) {
    }
}
