package org.weather.metricsapi.dto.update.v1;

import java.time.Instant;

public record UpdateResponse(
        String sensorId,
        Instant timestamp,
        int savedCount
) {}