package org.weather.metricsapi.dto.fetch.v1;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record FetchRequest(
        @Size(max = 100, message = "Maximum 100 sensors can be queried at once")
        List<@Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "sensorId must contain only alphanumeric characters, hyphens, and underscores") String> sensorId,

        @NotEmpty(message = "At least one metric must be specified")
        @Size(max = 20, message = "Maximum 20 metrics can be queried at once")
        List<@NotNull(message = "Metric name cannot be null") String> metrics,

        @NotNull(message = "Statistic type is required")
        @Pattern(regexp = "^(min|max|sum|average)$",
                message = "Statistic must be one of: min, max, sum, average")
        String statistic,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate
) {}