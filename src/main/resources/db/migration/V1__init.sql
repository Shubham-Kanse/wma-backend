create table snapshots (
                           id uuid primary key,
                           sensor_id text not null,
                           ts timestamptz not null,
                           received_at timestamptz not null default now(),
                           unique(sensor_id, ts)
);

create index idx_snapshots_sensor_ts on snapshots(sensor_id, ts);

create table reading_values (
                                id uuid primary key,
                                snapshot_id uuid not null references snapshots(id) on delete cascade,
                                sensor_id text not null,
                                ts timestamptz not null,
                                metric text not null,
                                value double precision not null,
                                unique(snapshot_id, metric),
                                unique(sensor_id, ts, metric)
);

create index idx_values_sensor_ts        on reading_values(sensor_id, ts);
create index idx_values_metric_ts        on reading_values(metric, ts);
create index idx_values_sensor_metric_ts on reading_values(sensor_id, metric, ts);
