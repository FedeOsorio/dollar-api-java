package com.dollarapi.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResilienceConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerListeners() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("dollarApi");
        cb.getEventPublisher()
                .onStateTransition(event -> log.warn("⚡ [CIRCUIT BREAKER] State transition: {} -> {}",
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                .onError(event -> log.warn("⚠️ [CIRCUIT BREAKER] Recorded error: duration={}ms, error={}",
                        event.getElapsedDuration().toMillis(), event.getThrowable().getMessage()))
                .onCallNotPermitted(event -> log.warn("⛔ [CIRCUIT BREAKER] Call rejected (Circuit is OPEN) - triggering fallback!"))
                .onSuccess(event -> log.info("✅ [CIRCUIT BREAKER] Call succeeded in {}ms",
                        event.getElapsedDuration().toMillis()));

        Retry retry = retryRegistry.retry("dollarApi");
        retry.getEventPublisher()
                .onRetry(event -> log.warn("🔁 [RETRY] Attempt #{} failed. Waiting for next attempt... Cause: {}",
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
    }
}
