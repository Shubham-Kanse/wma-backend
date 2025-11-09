package org.weather.metricsapi.dto.fetch.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FetchResponse(
        QueryInfo query,
        List<SensorResult> results
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QueryInfo(
            List<String> sensorId,
            List<String> metrics,
            String statistic,
            LocalDate startDate,
            LocalDate endDate,
            int totalSensors,
            int totalDataPoints
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SensorResult(
            String sensorId,
            Map<String, MetricStatistic> metrics
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MetricStatistic(
            String metric,
            String statistic,
            Double value,
            Integer dataPoints
    ) {}
}