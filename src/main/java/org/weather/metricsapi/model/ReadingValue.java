package org.weather.metricsapi.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reading_values",
        indexes = {
                @Index(name = "idx_values_sensor_ts", columnList = "sensor_id,ts"),
                @Index(name = "idx_values_metric_ts", columnList = "metric,ts"),
                @Index(name = "idx_values_sensor_metric_ts", columnList = "sensor_id,metric,ts")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_snapshot_metric", columnNames = {"snapshot_id", "metric"}),
                @UniqueConstraint(name = "uk_sensor_ts_metric", columnNames = {"sensor_id", "ts", "metric"})
        })
public class ReadingValue {

    @Id
    private UUID id;

    @Column(name = "snapshot_id", nullable = false)
    private UUID snapshotId;

    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Column(name = "metric", nullable = false)
    private String metric;

    @Column(name = "value", nullable = false)
    private Double value;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSnapshotId() { return snapshotId; }
    public void setSnapshotId(UUID snapshotId) { this.snapshotId = snapshotId; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
}
