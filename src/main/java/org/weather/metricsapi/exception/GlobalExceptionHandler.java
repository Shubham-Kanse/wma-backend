package org.weather.metricsapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.weather.metricsapi.dto.error.ApiError;
import org.weather.metricsapi.error.ErrorCodes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(),
                    error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value");
        }

        String traceId = getTraceId();

        log.warn("Validation failed [traceId={}]: {} field errors - {}",
                traceId, fieldErrors.size(), fieldErrors);

        ApiError apiError = ApiError.of(
                ErrorCodes.VALIDATION_FAILED.name(),
                "Request validation failed. Please check the field errors.",
                traceId,
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        String traceId = getTraceId();
        String message = "Malformed JSON request";

        if (ex.getCause() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage != null && causeMessage.contains("JSON parse error")) {
                message = "Invalid JSON format. Please check your request body.";
            }
        }

        log.warn("Malformed request [traceId={}]: {}", traceId, ex.getMessage());

        ApiError apiError = ApiError.of(
                ErrorCodes.BAD_REQUEST.name(),
                message,
                traceId,
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {

        String traceId = getTraceId();

        log.warn("Method not supported [traceId={}]: {} - supported: {}",
                traceId, ex.getMethod(), ex.getSupportedHttpMethods());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", ex.getMethod());
        details.put("supportedMethods", ex.getSupportedHttpMethods());

        ApiError apiError = ApiError.of(
                ErrorCodes.METHOD_NOT_ALLOWED.name(),
                "HTTP method not supported for this endpoint",
                traceId,
                details
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(apiError);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request) {

        String traceId = getTraceId();

        log.warn("Unsupported media type [traceId={}]: {} - supported: {}",
                traceId, ex.getContentType(), ex.getSupportedMediaTypes());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("contentType", ex.getContentType());
        details.put("supportedMediaTypes", ex.getSupportedMediaTypes());

        ApiError apiError = ApiError.of(
                ErrorCodes.UNSUPPORTED_MEDIA_TYPE.name(),
                "Content-Type not supported. Please use application/json",
                traceId,
                details
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        String traceId = getTraceId();

        log.warn("Invalid argument [traceId={}]: {}", traceId, ex.getMessage());

        ApiError apiError = ApiError.of(
                ErrorCodes.BAD_REQUEST.name(),
                ex.getMessage(),
                traceId,
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            WebRequest request) {

        String traceId = getTraceId();

        log.error("Unexpected error [traceId={}]: {}", traceId, ex.getMessage(), ex);

        ApiError apiError = ApiError.of(
                ErrorCodes.INTERNAL_ERROR.name(),
                "An unexpected error occurred. Please contact support with the trace ID.",
                traceId,
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}