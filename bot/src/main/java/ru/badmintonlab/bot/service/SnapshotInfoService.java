package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.entity.SnapshotMeta;
import ru.badmintonlab.core.repository.SnapshotMetaRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Optional;

/**
 * Дата актуальности данных для футера «Данные на DD.MM.YYYY».
 */
@Service
public class SnapshotInfoService {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    private final SnapshotMetaRepository snapshotMetaRepository;

    public SnapshotInfoService(SnapshotMetaRepository snapshotMetaRepository) {
        this.snapshotMetaRepository = snapshotMetaRepository;
    }

    /** Дата последнего слепка (по всем регионам берём самый свежий). */
    @Transactional(readOnly = true)
    public Optional<LocalDate> lastSnapshotDate() {
        return snapshotMetaRepository.findAll().stream()
                .map(SnapshotMeta::getLastSyncAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(instant -> instant.atZone(MOSCOW).toLocalDate());
    }
}
