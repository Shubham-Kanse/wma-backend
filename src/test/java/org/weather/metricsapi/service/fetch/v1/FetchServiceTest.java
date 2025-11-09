package org.weather.metricsapi.service.fetch.v1;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.weather.metricsapi.dto.fetch.v1.FetchRequest;
import org.weather.metricsapi.dto.fetch.v1.FetchResponse;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Tuple> query;

    @Mock
    private Tuple tuple;

    private Clock clock;
    private FetchService fetchService;

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 1, 15);
    private static final Instant TEST_INSTANT = TEST_DATE.atStartOfDay(ZoneId.of("UTC")).toInstant();

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(TEST_INSTANT, ZoneId.of("UTC"));

        fetchService = new FetchService(entityManager, clock);

        lenient().when(entityManager.createQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        lenient().when(query.getResultList()).thenReturn(List.of());
    }

    @Test
    void query_shouldReturnCorrectStatistics() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        when(tuple.get("sensorId", String.class)).thenReturn("sensor-001");
        when(tuple.get("metric", String.class)).thenReturn("temperature");
        when(tuple.get("value", Double.class)).thenReturn(25.5);
        when(tuple.get("dataPoints")).thenReturn(10);
        when(query.getResultList()).thenReturn(List.of(tuple));

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        assertEquals(1, response.results().size());
        assertEquals("sensor-001", response.results().get(0).sensorId());
        assertEquals(25.5, response.results().get(0).metrics().get("temperature").value());
        assertEquals("average", response.results().get(0).metrics().get("temperature").statistic());
        assertEquals(10, response.results().get(0).metrics().get("temperature").dataPoints());

        assertEquals("average", response.query().statistic());
        assertEquals(TEST_DATE.minusDays(7), response.query().startDate());
        assertEquals(TEST_DATE, response.query().endDate());
        assertEquals(1, response.query().totalSensors());
        assertEquals(10, response.query().totalDataPoints());
    }

    @Test
    void query_shouldValidateDateRange() {
        FetchRequest invalidRange = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                TEST_DATE,
                TEST_DATE.minusDays(7)
        );

        IllegalArgumentException exception1 = assertThrows(
                IllegalArgumentException.class,
                () -> fetchService.query(invalidRange)
        );
        assertTrue(exception1.getMessage().contains("startDate must be before or equal to endDate"));

        FetchRequest tooLong = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                TEST_DATE.minusDays(32),
                TEST_DATE
        );

        IllegalArgumentException exception2 = assertThrows(
                IllegalArgumentException.class,
                () -> fetchService.query(tooLong)
        );
        assertTrue(exception2.getMessage().contains("Date range must not exceed 31 days"));
    }

    @Test
    void query_shouldRejectInvalidStatistic() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "invalid",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fetchService.query(request)
        );
        assertTrue(exception.getMessage().contains("Statistic must be one of"));
    }

    @Test
    void query_shouldHandleMultipleSensors() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001", "sensor-002"),
                List.of("temperature"),
                "max",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        Tuple tuple1 = mock(Tuple.class);
        when(tuple1.get("sensorId", String.class)).thenReturn("sensor-001");
        when(tuple1.get("metric", String.class)).thenReturn("temperature");
        when(tuple1.get("value", Double.class)).thenReturn(30.0);
        when(tuple1.get("dataPoints")).thenReturn(5);

        Tuple tuple2 = mock(Tuple.class);
        when(tuple2.get("sensorId", String.class)).thenReturn("sensor-002");
        when(tuple2.get("metric", String.class)).thenReturn("temperature");
        when(tuple2.get("value", Double.class)).thenReturn(28.0);
        when(tuple2.get("dataPoints")).thenReturn(5);

        when(query.getResultList()).thenReturn(List.of(tuple1, tuple2));

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        assertEquals(2, response.results().size());
        assertEquals(2, response.query().totalSensors());
        assertEquals(10, response.query().totalDataPoints());
    }

    @Test
    void query_shouldUseDefaultDateRangeWhenNotProvided() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                null,
                null
        );

        when(query.getResultList()).thenReturn(List.of());

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        assertEquals(TEST_DATE, response.query().endDate());
        assertEquals(TEST_DATE.minusDays(7), response.query().startDate());
    }

    @Test
    void query_shouldHandleAllSensorsWhenSensorIdNull() {
        FetchRequest request = new FetchRequest(
                null, // null sensor IDs means all sensors
                List.of("temperature"),
                "sum",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        when(tuple.get("sensorId", String.class)).thenReturn("sensor-001");
        when(tuple.get("metric", String.class)).thenReturn("temperature");
        when(tuple.get("value", Double.class)).thenReturn(100.0);
        when(tuple.get("dataPoints")).thenReturn(10);
        when(query.getResultList()).thenReturn(List.of(tuple));

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        assertNull(response.query().sensorId()); // Should be null in response when querying all
        assertEquals(1, response.results().size());
    }

    @Test
    void query_shouldHandleMinStatistic() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "min",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        when(tuple.get("sensorId", String.class)).thenReturn("sensor-001");
        when(tuple.get("metric", String.class)).thenReturn("temperature");
        when(tuple.get("value", Double.class)).thenReturn(18.5);
        when(tuple.get("dataPoints")).thenReturn(10);
        when(query.getResultList()).thenReturn(List.of(tuple));

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        FetchResponse.MetricStatistic stat = response.results().get(0).metrics().get("temperature");
        assertEquals("min", stat.statistic());
        assertEquals(18.5, stat.value());
    }

    @Test
    void query_shouldHandleMultipleMetrics() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature", "humidity"),
                "average",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        Tuple tuple1 = mock(Tuple.class);
        when(tuple1.get("sensorId", String.class)).thenReturn("sensor-001");
        when(tuple1.get("metric", String.class)).thenReturn("temperature");
        when(tuple1.get("value", Double.class)).thenReturn(25.5);
        when(tuple1.get("dataPoints")).thenReturn(10);

        Tuple tuple2 = mock(Tuple.class);
        when(tuple2.get("sensorId", String.class)).thenReturn("sensor-001");
        when(tuple2.get("metric", String.class)).thenReturn("humidity");
        when(tuple2.get("value", Double.class)).thenReturn(65.0);
        when(tuple2.get("dataPoints")).thenReturn(10);

        when(query.getResultList()).thenReturn(List.of(tuple1, tuple2));

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        assertEquals(1, response.results().size());

        FetchResponse.SensorResult sensorResult = response.results().get(0);
        assertEquals("sensor-001", sensorResult.sensorId());
        assertEquals(2, sensorResult.metrics().size());

        FetchResponse.MetricStatistic tempStat = sensorResult.metrics().get("temperature");
        assertEquals("temperature", tempStat.metric());
        assertEquals("average", tempStat.statistic());
        assertEquals(25.5, tempStat.value());
        assertEquals(10, tempStat.dataPoints());

        FetchResponse.MetricStatistic humStat = sensorResult.metrics().get("humidity");
        assertEquals("humidity", humStat.metric());
        assertEquals("average", humStat.statistic());
        assertEquals(65.0, humStat.value());
        assertEquals(10, humStat.dataPoints());

        assertEquals(20, response.query().totalDataPoints());
    }

    @Test
    void query_shouldHandleEmptyResults() {
        FetchRequest request = new FetchRequest(
                List.of("sensor-001"),
                List.of("temperature"),
                "average",
                TEST_DATE.minusDays(7),
                TEST_DATE
        );

        when(query.getResultList()).thenReturn(List.of());

        FetchResponse response = fetchService.query(request);

        assertNotNull(response);
        assertEquals(0, response.results().size());
        assertEquals(0, response.query().totalSensors());
        assertEquals(0, response.query().totalDataPoints());
    }
}