 package com.dollarapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service health and resilience telemetry response")
public class HealthResponse {

    @Schema(description = "Overall service status", example = "UP")
    private String status;

    @Schema(description = "Service identifier", example = "dollar-api-java")
    private String service;

    @Schema(description = "Current server timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    @Schema(description = "Resilience4j Circuit Breaker metrics and state")
    private CircuitBreakerStatus circuitBreaker;

    @Schema(description = "Upstream provider status and simulation mode")
    private UpstreamStatus upstream;

    @Schema(description = "Cache provider and connectivity status")
    private CacheStatus cache;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Circuit breaker details")
    public static class CircuitBreakerStatus {
        @Schema(description = "State of Circuit Breaker", example = "CLOSED", allowableValues = {"CLOSED", "OPEN", "HALF_OPEN", "DISABLED", "FORCED_OPEN"})
        private String state;

        @Schema(description = "Configured failure rate threshold percentage", example = "50.0")
        private float failureRateThreshold;

        @Schema(description = "Current calculated failure rate percentage", example = "0.0")
        private float failureRate;

        @Schema(description = "Current slow call rate percentage", example = "0.0")
        private float slowCallRate;

        @Schema(description = "Total number of buffered calls in sliding window", example = "6")
        private int bufferedCalls;

        @Schema(description = "Number of failed calls in sliding window", example = "0")
        private int failedCalls;

        @Schema(description = "Number of successful calls in sliding window", example = "6")
        private int successfulCalls;

        @Schema(description = "Number of rejected calls when circuit was OPEN", example = "0")
        private long notPermittedCalls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Upstream provider details")
    public static class UpstreamStatus {
        @Schema(description = "Configured primary upstream endpoint", example = "https://dolarapi.com/v1/dolares")
        private String provider;

        @Schema(description = "Upstream health status", example = "UP")
        private String status;

        @Schema(description = "Active chaos simulation mode", example = "NONE")
        private ChaosMode chaosMode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Cache operational status")
    public static class CacheStatus {
        @Schema(description = "Cache health status", example = "CONNECTED")
        private String status;

        @Schema(description = "Active cache tier", example = "REDIS", allowableValues = {"REDIS", "IN_MEMORY"})
        private String type;

        @Schema(description = "Whether primary Redis instance is currently connected", example = "true")
        private boolean redisConnected;
    }
}
