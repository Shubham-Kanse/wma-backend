package org.weather.metricsapi.controller.fetch.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.weather.metricsapi.dto.fetch.v1.FetchRequest;
import org.weather.metricsapi.model.ReadingValue;
import org.weather.metricsapi.model.Snapshot;
import org.weather.metricsapi.repository.ReadingValueRepo;
import org.weather.metricsapi.repository.SnapshotRepo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FetchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SnapshotRepo snapshotRepo;

    @Autowired
    private ReadingValueRepo readingValueRepo;

    @BeforeEach
    void setUp() {
        readingValueRepo.deleteAll();
        snapshotRepo.deleteAll();
    }

    @Test
    void fetch_shouldReturnAverageForSingleSensor() throws Exception {
        LocalDate today = LocalDate.now();
        Instant timestamp = today.atStartOfDay(ZoneId.of("UTC")).toInstant();

        insertTestDataWithTimestamp("sensor-001", 25.0, 60.0, timestamp);

        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                today,
                today
        );
        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.statistic").value("average"))
                .andExpect(jsonPath("$.query.metrics[0]").value("temperature"))
                .andExpect(jsonPath("$.results[0].sensorId").value("sensor-001"))
                .andExpect(jsonPath("$.results[0].metrics.temperature.value").value(25.0))
                .andExpect(jsonPath("$.results[0].metrics.temperature.dataPoints").value(1));
    }

    @Test
    void fetch_shouldHandleMultipleSensorsAndMetrics() throws Exception {
        LocalDate today = LocalDate.now();
        Instant timestamp = today.atStartOfDay(ZoneId.of("UTC")).toInstant();

        insertTestDataWithTimestamp("sensor-001", 25.0, 60.0, timestamp);
        insertTestDataWithTimestamp("sensor-002", 22.0, 65.0, timestamp);

        FetchRequest request = new FetchRequest(
                List.of("sensor-001", "sensor-002"),
                List.of("temperature", "humidity"),
                "max",
                today,
                today
        );
        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.totalSensors").value(2))
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].metrics.temperature").exists())
                .andExpect(jsonPath("$.results[0].metrics.humidity").exists());
    }

    @Test
    void fetch_shouldSupportAllStatisticTypes() throws Exception {
        LocalDate today = LocalDate.now();
        Instant timestamp1 = today.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant timestamp2 = today.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant();

        insertTestDataWithTimestamp("sensor-001", 20.0, 50.0, timestamp1);
        insertTestDataWithTimestamp("sensor-001", 30.0, 70.0, timestamp2);

        String[] statistics = {"min", "max", "sum", "average"};
        Double[] expectedValues = {20.0, 30.0, 50.0, 25.0};

        for (int i = 0; i < statistics.length; i++) {
            FetchRequest request = new FetchRequest(
                    List.of("sensor-001"),
                    List.of("temperature"),
                    statistics[i],
                    today,
                    today
            );

            mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query.statistic").value(statistics[i]))
                    .andExpect(jsonPath("$.results[0].metrics.temperature.value").value(expectedValues[i]))
                    .andExpect(jsonPath("$.results[0].metrics.temperature.dataPoints").value(2));
        }
    }

    @Test
    void fetch_shouldValidateDateRange() throws Exception {
        FetchRequest invalidRange = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                LocalDate.now(),
                LocalDate.now().minusDays(7)
        );

        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRange)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        FetchRequest tooLong = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                LocalDate.now().minusDays(32),
                LocalDate.now()
        );

        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("31 days")));
    }

    @Test
    void fetch_shouldRejectInvalidRequests() throws Exception {
        String missingMetrics = """
            {"sensorId": ["sensor-001"], "statistic": "average"}
            """;

        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingMetrics))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        FetchRequest invalidStat = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "invalid_stat",
                LocalDate.now().minusDays(7),
                LocalDate.now()
        );

        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void fetch_shouldReturnEmptyForNonExistentSensor() throws Exception {
        FetchRequest request = new FetchRequest(
                List.of("non-existent-sensor"),
                List.of("temperature"),
                "average",
                LocalDate.now().minusDays(7),
                LocalDate.now()
        );

        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(0)))
                .andExpect(jsonPath("$.query.totalSensors").value(0));
    }

    @Test
    void fetch_shouldUseDefaultDateRangeWhenNotProvided() throws Exception {
        LocalDate today = LocalDate.now();
        Instant timestamp = today.minusDays(3).atStartOfDay(ZoneId.of("UTC")).toInstant();
        insertTestDataWithTimestamp("sensor-001", 25.0, 60.0, timestamp);

        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                null,  // No start date
                null   // No end date
        );

        mockMvc.perform(post("/api/weather/metrics/v1/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.startDate").exists())
                .andExpect(jsonPath("$.query.endDate").exists())
                .andExpect(jsonPath("$.results[0].sensorId").value("sensor-001"));
    }

    private void insertTestDataWithTimestamp(String sensorId, Double temperature, Double humidity, Instant timestamp) {
        UUID snapshotId = UUID.randomUUID();

        Snapshot snapshot = new Snapshot();
        snapshot.setId(snapshotId);
        snapshot.setSensorId(sensorId);
        snapshot.setTs(timestamp);
        snapshotRepo.save(snapshot);

        if (temperature != null) {
            ReadingValue tempValue = new ReadingValue();
            tempValue.setId(UUID.randomUUID());
            tempValue.setSnapshotId(snapshotId);
            tempValue.setSensorId(sensorId);
            tempValue.setTs(timestamp);
            tempValue.setMetric("temperature");
            tempValue.setValue(temperature);
            readingValueRepo.save(tempValue);
        }

        if (humidity != null) {
            ReadingValue humValue = new ReadingValue();
            humValue.setId(UUID.randomUUID());
            humValue.setSnapshotId(snapshotId);
            humValue.setSensorId(sensorId);
            humValue.setTs(timestamp);
            humValue.setMetric("humidity");
            humValue.setValue(humidity);
            readingValueRepo.save(humValue);
        }
    }
}