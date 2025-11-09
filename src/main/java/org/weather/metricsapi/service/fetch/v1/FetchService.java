package org.weather.metricsapi.service.fetch.v1;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.weather.metricsapi.dto.fetch.v1.FetchRequest;
import org.weather.metricsapi.dto.fetch.v1.FetchResponse;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FetchService {

    private static final Logger log = LoggerFactory.getLogger(FetchService.class);
    private final EntityManager entityManager;
    private final Clock clock;
    private static final Set<String> VALID_STATISTICS = Set.of("min", "max", "sum", "average");

    public FetchService(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FetchResponse query(FetchRequest request) {
        log.debug("Processing fetch query: {}", request);

        validateStatistic(request.statistic());

        LocalDate endDate = request.endDate() != null
                ? request.endDate()
                : LocalDate.now(clock);
        LocalDate startDate = request.startDate() != null
                ? request.startDate()
                : endDate.minusDays(7);

        log.debug("Resolved date range: {} to {}", startDate, endDate);

        validateDateRange(startDate, endDate);
        Instant startInstant = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        String aggregateFunction = getAggregateFunction(request.statistic());
        log.debug("Executing query with aggregate function: {}", aggregateFunction);

        List<Tuple> rawResults = executeQuery(
                request.sensorId(),
                request.metrics(),
                aggregateFunction,
                startInstant,
                endInstant
        );

        log.debug("Query returned {} raw result rows", rawResults.size());

        Map<String, Map<String, FetchResponse.MetricStatistic>> groupedResults = groupResultsBySensor(
                rawResults,
                request.statistic()
        );

        List<FetchResponse.SensorResult> sensorResults = groupedResults.entrySet().stream()
                .map(entry -> new FetchResponse.SensorResult(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        int totalDataPoints = rawResults.stream()
                .mapToInt(t -> ((Number) t.get("dataPoints")).intValue())
                .sum();

        log.info("Fetch query successful: {} sensors, {} metrics, {} data points processed",
                sensorResults.size(), request.metrics().size(), totalDataPoints);

        FetchResponse.QueryInfo queryInfo = new FetchResponse.QueryInfo(
                request.sensorId() != null && !request.sensorId().isEmpty()
                        ? request.sensorId()
                        : null,
                request.metrics(),
                request.statistic(),
                startDate,
                endDate,
                sensorResults.size(),
                totalDataPoints
        );

        return new FetchResponse(queryInfo, sensorResults);
    }

    private void validateStatistic(String statistic) {
        if (statistic == null || !VALID_STATISTICS.contains(statistic.toLowerCase())) {
            log.warn("Invalid statistic requested: {}", statistic);
            throw new IllegalArgumentException(
                    "Statistic must be one of: min, max, sum, average"
            );
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            log.warn("Invalid date range: start={} is after end={}", start, end);
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (daysBetween < 1) {
            log.warn("Date range too short: {} days", daysBetween);
            throw new IllegalArgumentException("Date range must be at least 1 day");
        }
        if (daysBetween > 31) {
            log.warn("Date range too long: {} days", daysBetween);
            throw new IllegalArgumentException("Date range must not exceed 31 days");
        }
    }

    private String getAggregateFunction(String statistic) {
        return switch (statistic.toLowerCase()) {
            case "min" -> "MIN";
            case "max" -> "MAX";
            case "sum" -> "SUM";
            case "average" -> "AVG";
            default -> throw new IllegalArgumentException("Invalid statistic: " + statistic);
        };
    }

    private List<Tuple> executeQuery(
            List<String> sensorId,
            List<String> metrics,
            String aggregateFunction,
            Instant startInstant,
            Instant endInstant
    ) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT rv.sensorId AS sensorId, ")
                .append("rv.metric AS metric, ")
                .append(aggregateFunction).append("(rv.value) AS value, ")
                .append("COUNT(rv.id) AS dataPoints ")
                .append("FROM ReadingValue rv ")
                .append("WHERE rv.ts >= :startDate AND rv.ts < :endDate ");

        if (sensorId != null && !sensorId.isEmpty()) {
            jpql.append("AND rv.sensorId IN :sensorId ");
        }

        jpql.append("AND rv.metric IN :metrics ")
                .append("GROUP BY rv.sensorId, rv.metric ")
                .append("ORDER BY rv.sensorId, rv.metric");

        log.debug("Executing JPQL: {}", jpql.toString());
        log.debug("Parameters: startDate={}, endDate={}, metrics={}, sensorId={}",
                startInstant, endInstant, metrics, sensorId);

        TypedQuery<Tuple> query = entityManager.createQuery(jpql.toString(), Tuple.class);
        query.setParameter("startDate", startInstant);
        query.setParameter("endDate", endInstant);
        query.setParameter("metrics", metrics);

        if (sensorId != null && !sensorId.isEmpty()) {
            query.setParameter("sensorId", sensorId);
        }

        return query.getResultList();
    }

    private Map<String, Map<String, FetchResponse.MetricStatistic>> groupResultsBySensor(
            List<Tuple> results,
            String statistic
    ) {
        Map<String, Map<String, FetchResponse.MetricStatistic>> grouped = new LinkedHashMap<>();

        for (Tuple tuple : results) {
            String sensorId = tuple.get("sensorId", String.class);
            String metric = tuple.get("metric", String.class);
            Double value = tuple.get("value", Double.class);
            Integer dataPoints = ((Number) tuple.get("dataPoints")).intValue();

            grouped.computeIfAbsent(sensorId, k -> new LinkedHashMap<>())
                    .put(metric, new FetchResponse.MetricStatistic(
                            metric,
                            statistic,
                            value,
                            dataPoints
                    ));
        }

        log.debug("Grouped results into {} sensors", grouped.size());
        return grouped;
    }
}