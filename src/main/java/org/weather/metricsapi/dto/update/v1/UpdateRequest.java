package org.weather.metricsapi.dto.update.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRequest(
        @NotBlank(message = "sensorId is required and cannot be blank")
        @Size(min = 3, max = 50, message = "sensorId must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "sensorId must contain only alphanumeric characters, hyphens, and underscores")
        String sensorId,

        @Valid
        @NotNull(message = "metrics object is required")
        Metrics metrics
) {}