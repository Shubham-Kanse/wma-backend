package org.weather.metricsapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.weather.metricsapi.model.Snapshot;
import java.util.UUID;

public interface SnapshotRepo extends JpaRepository<Snapshot, UUID> {}
