package ru.badmintonlab.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.badmintonlab.core.entity.SnapshotMeta;

public interface SnapshotMetaRepository extends JpaRepository<SnapshotMeta, String> {
}
