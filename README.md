# Weather Metrics API

A REST API service for ingesting and querying weather sensor data with support for multiple metrics and statistical aggregations.

## Overview

This application provides a backend service for weather monitoring systems that:
- Receives real-time metric updates from weather sensors via API
- Stores time-series data in PostgreSQL
- Supports querying historical data with statistical aggregations (min, max, sum, average)
- Handles multiple sensors and various weather metrics (temperature, humidity, pressure, wind, etc.)

## Features

- **Data Ingestion**: POST endpoint to receive sensor readings
- **Flexible Querying**: Query by sensor(s), metric(s), date range, and statistic type
- **Input Validation**: Comprehensive validation with detailed error messages
- **Exception Handling**: Global error handling with trace IDs for debugging
- **Database Persistence**: PostgreSQL with Flyway migrations
- **Test Coverage**: Unit and integration tests for core functionality

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.7
- **Database**: PostgreSQL
- **Migration**: Flyway
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, Spring Test

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6+ (or use included Maven wrapper)

## Database Setup

1. Install PostgreSQL and create a database:

```sql
CREATE DATABASE weather;
CREATE USER weather WITH PASSWORD 'weather';
GRANT ALL PRIVILEGES ON DATABASE weather TO weather;
```

2. The application will automatically run Flyway migrations on startup to create the required tables.

### Database Schema

The application uses two main tables:

- `snapshots`: Stores sensor reading metadata (sensor ID, timestamp)
- `reading_values`: Stores individual metric values linked to snapshots

## Running the Application

### Using Maven Wrapper

```bash
# On Unix/Mac
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

### Using Maven

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Update Sensor Data

**Endpoint**: `POST /api/weather/metrics/v1/update`

**Description**: Submit new metric readings from a sensor

**Request Body**:
```json
{
  "sensorId": "sensor-001",
  "metrics": {
    "temperature": 25.5,
    "humidity": 65.0,
    "pressure": 1013.25,
    "windSpeed": 15.0,
    "windDirection": 180.0,
    "rainfall": 0.0,
    "uvIndex": 5.0,
    "aqi": 50.0
  }
}
```

**Required Fields**:
- `sensorId`: String (3-50 chars, alphanumeric with hyphens/underscores)
- `temperature`: Number (-100 to 100°C)
- `humidity`: Number (0 to 100%)

**Optional Fields**:
- `pressure`: Number (800-1200 hPa)
- `windSpeed`: Number (0-500 km/h)
- `windDirection`: Number (0-360 degrees)
- `rainfall`: Number (0-1000 mm)
- `uvIndex`: Number (0-20)
- `aqi`: Number (0-500)

**Response**:
```json
{
  "sensorId": "sensor-001",
  "timestamp": "2025-11-09T03:57:16.289Z",
  "savedCount": 8
}
```

**Example cURL**:
```bash
curl -X POST http://localhost:8080/api/weather/metrics/v1/update \
  -H "Content-Type: application/json" \
  -d '{
    "sensorId": "sensor-001",
    "metrics": {
      "temperature": 25.5,
      "humidity": 65.0
    }
  }'
```

### 2. Query Sensor Data

**Endpoint**: `POST /api/weather/metrics/v1/fetch`

**Description**: Query historical sensor data with statistical aggregations

**Request Body**:
```json
{
  "sensorId": ["sensor-001", "sensor-002"],
  "metrics": ["temperature", "humidity"],
  "statistic": "average",
  "startDate": "2025-11-02",
  "endDate": "2025-11-09"
}
```

**Fields**:
- `sensorId`: Array of sensor IDs (optional, omit for all sensors, max 100)
- `metrics`: Array of metric names (required, max 20)
- `statistic`: One of `min`, `max`, `sum`, `average` (required)
- `startDate`: ISO date format YYYY-MM-DD (optional, defaults to 7 days ago)
- `endDate`: ISO date format YYYY-MM-DD (optional, defaults to today)

**Date Range Constraints**:
- Must be between 1 and 31 days
- `startDate` must be before or equal to `endDate`

**Response**:
```json
{
  "query": {
    "sensorId": ["sensor-001", "sensor-002"],
    "metrics": ["temperature", "humidity"],
    "statistic": "average",
    "startDate": "2025-11-02",
    "endDate": "2025-11-09",
    "totalSensors": 2,
    "totalDataPoints": 20
  },
  "results": [
    {
      "sensorId": "sensor-001",
      "metrics": {
        "temperature": {
          "metric": "temperature",
          "statistic": "average",
          "value": 24.5,
          "dataPoints": 10
        },
        "humidity": {
          "metric": "humidity",
          "statistic": "average",
          "value": 62.3,
          "dataPoints": 10
        }
      }
    }
  ]
}
```

**Example Queries**:

```bash
# Get average temperature for sensor-001 in the last week
curl -X POST http://localhost:8080/api/weather/metrics/v1/fetch \
  -H "Content-Type: application/json" \
  -d '{
    "sensorId": ["sensor-001"],
    "metrics": ["temperature"],
    "statistic": "average"
  }'

# Get max temperature and humidity for all sensors in date range
curl -X POST http://localhost:8080/api/weather/metrics/v1/fetch \
  -H "Content-Type: application/json" \
  -d '{
    "metrics": ["temperature", "humidity"],
    "statistic": "max",
    "startDate": "2025-11-01",
    "endDate": "2025-11-09"
  }'
```

## Error Handling

The API returns structured error responses with trace IDs for debugging:

```json
{
  "status": "error",
  "errorCode": "VALIDATION_FAILED",
  "message": "Request validation failed. Please check the field errors.",
  "traceId": "588de978-199a-4aa4-ad8a-976f39175a64",
  "details": {
    "temperature": "temperature must be at least -100°C"
  }
}
```

**Error Codes**:
- `VALIDATION_FAILED`: Invalid request parameters
- `BAD_REQUEST`: Malformed request
- `METHOD_NOT_ALLOWED`: Unsupported HTTP method
- `UNSUPPORTED_MEDIA_TYPE`: Wrong Content-Type
- `INTERNAL_ERROR`: Unexpected server error

## Configuration

Application configuration is in `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/weather
spring.datasource.username=weather
spring.datasource.password=weather

# Logging
logging.level.org.weather.metricsapi=INFO
logging.file.name=logs/weather-metrics-api.log
```

## Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw verify

# Run specific test class
./mvnw test -Dtest=FetchControllerTest
```

## Project Structure

```
src/
├── main/
│   ├── java/org/weather/metricsapi/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Request/Response objects
│   │   ├── exception/        # Error handling
│   │   ├── filter/           # Request filters
│   │   ├── model/            # JPA entities
│   │   ├── repository/       # Data access
│   │   └── service/          # Business logic
│   └── resources/
│       ├── application.properties
│       └── db/migration/     # Flyway SQL scripts
└── test/
    └── java/org/weather/metricsapi/
        ├── controller/       # Integration tests
        └── service/          # Unit tests
```

## Implementation Notes

### Database Design
- **Snapshot Model**: Each sensor reading creates a snapshot record with a unique timestamp
- **Value Storage**: Individual metrics are stored as separate rows for flexible querying
- **Indexes**: Composite indexes on sensor_id, metric, and timestamp for query performance
- **Constraints**: Unique constraints prevent duplicate readings at the same timestamp

### API Design
- **POST for Queries**: Using POST instead of GET for complex query parameters
- **Versioned Endpoints**: `/v1/` prefix allows for future API evolution
- **Stateless**: No session management, fully RESTful

### Validation
- **Bean Validation**: Using Jakarta validation annotations for declarative validation
- **Range Checks**: Realistic bounds on all metric values
- **Pattern Matching**: Alphanumeric sensor IDs only

### Areas for Future Enhancement
- Authentication and authorization
- Batch insert optimization for high-volume ingestion
- Query result pagination for large datasets
- Real-time WebSocket streaming
- Data retention and archival policies
- Sensor registration and metadata management
- Alerting based on threshold rules
- Metric unit conversion
