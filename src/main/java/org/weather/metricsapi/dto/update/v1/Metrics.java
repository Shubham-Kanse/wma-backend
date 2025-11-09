package org.weather.metricsapi.dto.update.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Metrics(
        @NotNull(message = "temperature is required")
        @DecimalMin(value = "-100.0", message = "temperature must be at least -100°C")
        @DecimalMax(value = "100.0", message = "temperature must not exceed 100°C")
        Double temperature,

        @NotNull(message = "humidity is required")
        @DecimalMin(value = "0.0", message = "humidity must be at least 0%")
        @DecimalMax(value = "100.0", message = "humidity must not exceed 100%")
        Double humidity,

        @DecimalMin(value = "800.0", message = "pressure must be at least 800 hPa")
        @DecimalMax(value = "1200.0", message = "pressure must not exceed 1200 hPa")
        Double pressure,

        @DecimalMin(value = "0.0", message = "windSpeed must be at least 0 km/h")
        @DecimalMax(value = "500.0", message = "windSpeed must not exceed 500 km/h")
        Double windSpeed,

        @DecimalMin(value = "0.0", message = "windDirection must be at least 0 degrees")
        @DecimalMax(value = "360.0", message = "windDirection must not exceed 360 degrees")
        Double windDirection,

        @DecimalMin(value = "0.0", message = "rainfall must be at least 0 mm")
        @DecimalMax(value = "1000.0", message = "rainfall must not exceed 1000 mm")
        Double rainfall,

        @DecimalMin(value = "0.0", message = "uvIndex must be at least 0")
        @DecimalMax(value = "20.0", message = "uvIndex must not exceed 20")
        Double uvIndex,

        @DecimalMin(value = "0.0", message = "aqi must be at least 0")
        @DecimalMax(value = "500.0", message = "aqi must not exceed 1000")
        Double aqi
) {}