package org.weather.metricsapi.controller.update.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.weather.metricsapi.dto.update.v1.Metrics;
import org.weather.metricsapi.dto.update.v1.UpdateRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void update_shouldAcceptValidRequestWithAllMetrics() throws Exception {
        Metrics metrics = new Metrics(25.5, 65.0, 1013.0, 15.0, 180.0, 0.5, 5.0, 50.0);
        UpdateRequest request = new UpdateRequest("sensor-001", metrics);

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value("sensor-001"))
                .andExpect(jsonPath("$.savedCount").value(8))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void update_shouldAcceptMinimalValidRequest() throws Exception {
        Metrics metrics = new Metrics(20.0, 50.0, null, null, null, null, null, null);
        UpdateRequest request = new UpdateRequest("sensor-minimal", metrics);

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value("sensor-minimal"))
                .andExpect(jsonPath("$.savedCount").value(2));
    }

    @Test
    void update_shouldRejectInvalidRequests() throws Exception {
        String missingId = """
            {"metrics": {"temperature": 25.0, "humidity": 60.0}}
            """;

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.sensorId").exists());

        String missingTemp = """
            {"sensorId": "sensor-001", "metrics": {"humidity": 60.0}}
            """;

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingTemp))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        Metrics outOfRange = new Metrics(150.0, 65.0, null, null, null, null, null, null);
        UpdateRequest request = new UpdateRequest("sensor-001", outOfRange);

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void update_shouldHandleMultipleConsecutiveRequests() throws Exception {
        Metrics metrics1 = new Metrics(20.0, 50.0, null, null, null, null, null, null);
        UpdateRequest request1 = new UpdateRequest("sensor-001", metrics1);

        Metrics metrics2 = new Metrics(25.0, 60.0, null, null, null, null, null, null);
        UpdateRequest request2 = new UpdateRequest("sensor-001", metrics2);

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/weather/metrics/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());
    }
}