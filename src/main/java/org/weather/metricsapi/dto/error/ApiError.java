package org.weather.metricsapi.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String status,
        String errorCode,
        String message,
        String traceId,
        Object details
) {
    public static ApiError of(String code, String msg, String traceId, Object details) {
        return new ApiError("error", code, msg, traceId, details);
    }
}
