package org.weather.metricsapi.service.update.v1;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.weather.metricsapi.dto.update.v1.Metrics;
import org.weather.metricsapi.dto.update.v1.UpdateRequest;
import org.weather.metricsapi.dto.update.v1.UpdateResponse;
import org.weather.metricsapi.model.ReadingValue;
import org.weather.metricsapi.model.Snapshot;
import org.weather.metricsapi.repository.ReadingValueRepo;
import org.weather.metricsapi.repository.SnapshotRepo;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateServiceTest {

    @Mock
    private SnapshotRepo snapshotRepo;

    @Mock
    private ReadingValueRepo valueRepo;

    @InjectMocks
    private UpdateService updateService;

    @Test
    void ingest_shouldSaveSnapshotAndAllMetrics() {
        Instant ts = Instant.parse("2025-01-15T10:30:00Z");
        Metrics metrics = new Metrics(25.5, 65.0, 1013.25, 15.0, 180.0, 0.0, 5.0, 50.0);
        UpdateRequest request = new UpdateRequest("sensor-001", metrics);

        when(snapshotRepo.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);
        when(valueRepo.save(any(ReadingValue.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateResponse response = updateService.ingest(ts, request);

        assertEquals("sensor-001", response.sensorId());
        assertEquals(8, response.savedCount());
        verify(snapshotRepo, times(1)).save(any(Snapshot.class));
        verify(valueRepo, times(8)).save(any(ReadingValue.class));
    }

    @Test
    void ingest_shouldSaveOnlyRequiredMetricsWhenOptionalAreNull() {
        Instant ts = Instant.now();
        Metrics metrics = new Metrics(22.0, 50.0, null, null, null, null, null, null);
        UpdateRequest request = new UpdateRequest("sensor-002", metrics);

        when(snapshotRepo.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);
        when(valueRepo.save(any(ReadingValue.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateResponse response = updateService.ingest(ts, request);

        assertEquals(2, response.savedCount());
        verify(valueRepo, times(2)).save(any(ReadingValue.class));
    }

    @Test
    void ingest_shouldMapMetricNamesCorrectly() {
        Instant ts = Instant.now();
        Metrics metrics = new Metrics(25.0, 65.0, null, 15.0, null, null, 5.0, null);
        UpdateRequest request = new UpdateRequest("sensor-003", metrics);

        ArgumentCaptor<ReadingValue> valueCaptor = ArgumentCaptor.forClass(ReadingValue.class);
        when(snapshotRepo.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);
        when(valueRepo.save(any(ReadingValue.class))).thenAnswer(i -> i.getArguments()[0]);

        updateService.ingest(ts, request);

        verify(valueRepo, times(4)).save(valueCaptor.capture());
        List<String> metricNames = valueCaptor.getAllValues().stream()
                .map(ReadingValue::getMetric)
                .toList();

        assertTrue(metricNames.contains("temperature"));
        assertTrue(metricNames.contains("humidity"));
        assertTrue(metricNames.contains("windSpeed"));
        assertTrue(metricNames.contains("uvIndex"));
    }
}