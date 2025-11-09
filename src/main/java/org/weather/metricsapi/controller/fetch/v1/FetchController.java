package org.weather.metricsapi.controller.fetch.v1;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.weather.metricsapi.dto.fetch.v1.FetchRequest;
import org.weather.metricsapi.dto.fetch.v1.FetchResponse;
import org.weather.metricsapi.service.fetch.v1.FetchService;

@RestController
@RequestMapping("/api/weather/metrics/v1")
public class FetchController {

    private static final Logger log = LoggerFactory.getLogger(FetchController.class);

    private final FetchService service;

    public FetchController(FetchService service) {
        this.service = service;
    }

    @PostMapping("/fetch")
    public ResponseEntity<FetchResponse> query(@Valid @RequestBody FetchRequest request) {

        log.info("Fetching request");
        log.debug("Fetching request {}", request);

        FetchResponse response = service.query(request);

        log.info("Fetch query completed");
        log.debug("Fetch response: {}", response);

        return ResponseEntity.ok(response);
    }
}