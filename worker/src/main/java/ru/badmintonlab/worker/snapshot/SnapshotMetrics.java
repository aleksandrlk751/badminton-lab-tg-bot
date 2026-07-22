package ru.badmintonlab.worker.snapshot;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Счётчики одного прогона слепка: турниры, игроки, вставленные матчи, ошибки, длительность.
 */
public class SnapshotMetrics {

    private final long startedAtNanos = System.nanoTime();
    private final AtomicInteger tournamentsDiscovered = new AtomicInteger();
    private final AtomicInteger tournamentsProcessed = new AtomicInteger();
    private final AtomicInteger playersProcessed = new AtomicInteger();
    private final AtomicInteger matchesInserted = new AtomicInteger();
    private final AtomicInteger rivalRows = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();

    public void addDiscovered(int count) {
        tournamentsDiscovered.addAndGet(count);
    }

    public void incTournament() {
        tournamentsProcessed.incrementAndGet();
    }

    public void incPlayer() {
        playersProcessed.incrementAndGet();
    }

    public void addMatches(int count) {
        matchesInserted.addAndGet(count);
    }

    public void setRivalRows(int count) {
        rivalRows.set(count);
    }

    public void incError() {
        errors.incrementAndGet();
    }

    public int errors() {
        return errors.get();
    }

    public Duration elapsed() {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos);
    }

    @Override
    public String toString() {
        Duration d = elapsed();
        return "турниров найдено=" + tournamentsDiscovered.get()
                + ", обработано=" + tournamentsProcessed.get()
                + ", игроков=" + playersProcessed.get()
                + ", матчей вставлено=" + matchesInserted.get()
                + ", rival_summary строк=" + rivalRows.get()
                + ", ошибок=" + errors.get()
                + ", длительность=" + d.toMinutes() + "m" + (d.toSecondsPart()) + "s";
    }
}
