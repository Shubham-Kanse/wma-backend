package org.weather.metricsapi.controller.update.v1;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.weather.metricsapi.dto.update.v1.UpdateRequest;
import org.weather.metricsapi.dto.update.v1.UpdateResponse;
import org.weather.metricsapi.service.update.v1.UpdateService;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/weather/metrics/v1")
public class UpdateController {

    private static final Logger log = LoggerFactory.getLogger(UpdateController.class);

    private final UpdateService service;
    private final Clock clock;

    public UpdateController(UpdateService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping("/update")
    public ResponseEntity<UpdateResponse> ingest(@Valid @RequestBody UpdateRequest req) {

        log.info("Received update request");
        log.debug("Update request {}", req);

        Instant ts = Instant.now(clock);
        UpdateResponse resp = service.ingest(ts, req);

        log.info("Successfully ingested");
        log.debug("Update response: {}", resp);

        return ResponseEntity.ok(resp);
    }
}