package com.dollarapi.service;

import com.dollarapi.client.DolarApiClient;
import com.dollarapi.model.Quote;
import com.dollarapi.model.QuoteResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientQuoteFetcher {

    private final DolarApiClient dolarApiClient;
    private final CacheService cacheService;

    @CircuitBreaker(name = "dollarApi", fallbackMethod = "fetchFallback")
    @Retry(name = "dollarApi")
    public QuoteResult fetchUpstream() {
        log.info("[FETCHER] Initiating resilient upstream call to Dollar API...");
        List<Quote> quotes = dolarApiClient.fetchQuotes();
        cacheService.saveQuotes(quotes);
        return new QuoteResult(quotes, "MISS");
    }

    /**
     * Fallback invoked when:
     * 1. Circuit Breaker is OPEN (CallNotPermittedException)
     * 2. All Retry attempts are exhausted due to upstream errors/timeouts
     */
    public QuoteResult fetchFallback(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("[CIRCUIT BREAKER OPEN] Fast-failing upstream call and serving STALE_FALLBACK. Reason: Circuit is OPEN!");
        } else {
            log.warn("[FALLBACK TRIGGERED] Upstream call failed after retries ({}). Serving STALE_FALLBACK!", t.getMessage());
        }

        return cacheService.getStaleQuotes()
                .map(stale -> new QuoteResult(stale, "STALE_FALLBACK", "Served from last known good cache: " + t.getMessage()))
                .orElseGet(() -> {
                    log.warn("[COLD START FALLBACK] No cached data available. Returning built-in emergency snapshot.");
                    return new QuoteResult(cacheService.getEmergencySnapshot(), "STALE_FALLBACK", "Served from emergency snapshot: " + t.getMessage());
                });
    }
}
