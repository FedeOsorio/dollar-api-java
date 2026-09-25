package com.dollarapi.controller;

import com.dollarapi.model.ChaosMode;
import com.dollarapi.model.HealthResponse;
import com.dollarapi.service.CacheService;
import com.dollarapi.service.ChaosService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health & Telemetry", description = "Monitor microservice health, Circuit Breaker state, upstream status and cache connectivity")
public class HealthController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ChaosService chaosService;
    private final CacheService cacheService;

    @Value("${dollar.upstream.url:https://dolarapi.com/v1/dolares}")
    private String upstreamUrl;

    @GetMapping
    @Operation(summary = "System Health and Resilience Telemetry",
            description = "Returns real-time operational status, Circuit Breaker state (CLOSED/OPEN/HALF_OPEN), "
                    + "failure metrics, active chaos simulation mode, and cache tier status.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service telemetry report",
                    content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    })
    public ResponseEntity<HealthResponse> getHealth() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("dollarApi");
        var metrics = cb.getMetrics();
        var config = cb.getCircuitBreakerConfig();
        String cbState = cb.getState().name();

        ChaosMode currentChaos = chaosService.getMode();
        boolean isCircuitOpen = "OPEN".equalsIgnoreCase(cbState) || "FORCED_OPEN".equalsIgnoreCase(cbState);

        String upstreamStatus = currentChaos == ChaosMode.OUTAGE ? "DOWN"
                : (currentChaos == ChaosMode.NONE ? (isCircuitOpen ? "UNAVAILABLE" : "UP") : "DEGRADED");

        String overallStatus = (isCircuitOpen || currentChaos == ChaosMode.OUTAGE) ? "DEGRADED" : "UP";

        boolean redisConnected = cacheService.isRedisAvailable();

        HealthResponse response = HealthResponse.builder()
                .status(overallStatus)
                .service("dollar-api-java")
                .timestamp(Instant.now())
                .circuitBreaker(HealthResponse.CircuitBreakerStatus.builder()
                        .state(cbState)
                        .failureRateThreshold(config.getFailureRateThreshold())
                        .failureRate(metrics.getFailureRate())
                        .slowCallRate(metrics.getSlowCallRate())
                        .bufferedCalls(metrics.getNumberOfBufferedCalls())
                        .failedCalls(metrics.getNumberOfFailedCalls())
                        .successfulCalls(metrics.getNumberOfSuccessfulCalls())
                        .notPermittedCalls(metrics.getNumberOfNotPermittedCalls())
                        .build())
                .upstream(HealthResponse.UpstreamStatus.builder()
                        .provider(upstreamUrl)
                        .status(upstreamStatus)
                        .chaosMode(currentChaos)
                        .build())
                .cache(HealthResponse.CacheStatus.builder()
                        .status(redisConnected ? "CONNECTED" : "FALLBACK_IN_MEMORY")
                        .type(cacheService.getCacheType())
                        .redisConnected(redisConnected)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }
}
