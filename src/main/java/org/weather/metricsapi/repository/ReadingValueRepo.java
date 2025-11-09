package org.weather.metricsapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.weather.metricsapi.model.ReadingValue;
import java.util.UUID;

public interface ReadingValueRepo extends JpaRepository<ReadingValue, UUID> {}
