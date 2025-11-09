package org.weather.metricsapi.service.update.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.weather.metricsapi.dto.update.v1.Metrics;
import org.weather.metricsapi.dto.update.v1.UpdateRequest;
import org.weather.metricsapi.dto.update.v1.UpdateResponse;
import org.weather.metricsapi.model.Snapshot;
import org.weather.metricsapi.model.ReadingValue;
import org.weather.metricsapi.repository.SnapshotRepo;
import org.weather.metricsapi.repository.ReadingValueRepo;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);
    private final SnapshotRepo snapshotRepo;
    private final ReadingValueRepo valueRepo;

    public UpdateService(SnapshotRepo snapshotRepo, ReadingValueRepo valueRepo) {
        this.snapshotRepo = snapshotRepo;
        this.valueRepo = valueRepo;
    }

    @Transactional
    public UpdateResponse ingest(Instant serverTs, UpdateRequest req) {
        String sensorId = req.sensorId().trim();
        Metrics m = req.metrics();

        log.debug("Starting ingestion for sensor: {} at timestamp: {}", sensorId, serverTs);

        Snapshot snapshot = new Snapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setSensorId(sensorId);
        snapshot.setTs(serverTs);
        snapshotRepo.save(snapshot);

        log.debug("Created snapshot: id={}, sensor={}", snapshot.getId(), sensorId);

        Map<String, Double> provided = getStringDoubleMap(m);

        log.debug("Saving {} metrics for sensor: {}", provided.size(), sensorId);

        int saved = 0;
        for (var e : provided.entrySet()) {
            ReadingValue rv = new ReadingValue();
            rv.setId(UUID.randomUUID());
            rv.setSnapshotId(snapshot.getId());
            rv.setSensorId(sensorId);
            rv.setTs(serverTs);
            rv.setMetric(e.getKey());
            rv.setValue(e.getValue());
            valueRepo.save(rv);
            saved++;

            log.trace("Saved reading: metric={}, value={}", e.getKey(), e.getValue());
        }

        log.info("Ingestion complete: sensor={}, snapshot={}, metrics_saved={}",
                sensorId, snapshot.getId(), saved);

        return new UpdateResponse(sensorId, serverTs, saved);
    }

    private static Map<String, Double> getStringDoubleMap(Metrics m) {
        Map<String, Double> provided = new LinkedHashMap<>();
        provided.put("temperature", m.temperature());
        provided.put("humidity", m.humidity());
        if (m.pressure() != null)      provided.put("pressure", m.pressure());
        if (m.windSpeed() != null)     provided.put("windSpeed", m.windSpeed());
        if (m.windDirection() != null) provided.put("windDirection", m.windDirection());
        if (m.rainfall() != null)      provided.put("rainfall", m.rainfall());
        if (m.uvIndex() != null)       provided.put("uvIndex", m.uvIndex());
        if (m.aqi() != null)           provided.put("aqi", m.aqi());
        return provided;
    }
}