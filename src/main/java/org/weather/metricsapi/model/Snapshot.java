package org.weather.metricsapi.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "snapshots",
        indexes = @Index(name = "idx_snapshots_sensor_ts", columnList = "sensor_id,ts"))
public class Snapshot {

    @Id
    private UUID id;

    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Column(name = "received_at", insertable = false, updatable = false)
    private Instant receivedAt; // DB default now()

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
