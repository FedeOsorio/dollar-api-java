package com.dollarapi;

import com.dollarapi.client.DolarApiClient;
import com.dollarapi.exception.UpstreamServiceException;
import com.dollarapi.model.Quote;
import com.dollarapi.model.QuoteResult;
import com.dollarapi.service.CacheService;
import com.dollarapi.service.QuoteService;
import com.dollarapi.service.ResilientQuoteFetcher;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class QuoteServiceResilienceTest {

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private ResilientQuoteFetcher resilientQuoteFetcher;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private DolarApiClient dolarApiClient;

    @MockBean
    private CacheService cacheService;

    private List<Quote> mockQuotes;

    @BeforeEach
    void setUp() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("dollarApi");
        cb.reset();

        mockQuotes = List.of(
                Quote.builder().code("blue").name("Blue").buy(1540.0).sell(1560.0).currency("USD").updatedAt(Instant.now()).build(),
                Quote.builder().code("oficial").name("Oficial").buy(1490.0).sell(1540.0).currency("USD").updatedAt(Instant.now()).build()
        );
    }

    @Test
    @DisplayName("Should return HIT when data is present in cache without calling upstream")
    void shouldReturnHitFromCache() {
        when(cacheService.getQuotes()).thenReturn(Optional.of(mockQuotes));

        QuoteResult result = quoteService.getAllQuotes();

        assertThat(result.getCacheStatus()).isEqualTo("HIT");
        assertThat(result.getQuotes()).hasSize(2);
        verify(dolarApiClient, times(0)).fetchQuotes();
    }

    @Test
    @DisplayName("Should fetch from upstream and return MISS on cache miss")
    void shouldFetchFromUpstreamOnCacheMiss() {
        when(cacheService.getQuotes()).thenReturn(Optional.empty());
        when(dolarApiClient.fetchQuotes()).thenReturn(mockQuotes);

        QuoteResult result = quoteService.getAllQuotes();

        assertThat(result.getCacheStatus()).isEqualTo("MISS");
        assertThat(result.getQuotes()).hasSize(2);
        verify(dolarApiClient, times(1)).fetchQuotes();
        verify(cacheService, times(1)).saveQuotes(mockQuotes);
    }

    @Test
    @DisplayName("Should retry 3 times on upstream failure and fall back to stale cache with STALE_FALLBACK")
    void shouldRetryAndFallbackToStaleCache() {
        when(cacheService.getQuotes()).thenReturn(Optional.empty());
        when(dolarApiClient.fetchQuotes()).thenThrow(new UpstreamServiceException("503 Outage", 503));
        when(cacheService.getStaleQuotes()).thenReturn(Optional.of(mockQuotes));

        QuoteResult result = quoteService.getAllQuotes();

        assertThat(result.getCacheStatus()).isEqualTo("STALE_FALLBACK");
        assertThat(result.getQuotes()).hasSize(2);
        // Verify Retry attempted 3 times
        verify(dolarApiClient, times(3)).fetchQuotes();
    }

    @Test
    @DisplayName("Should fall back to emergency snapshot when upstream fails and no stale cache is present")
    void shouldFallbackToEmergencySnapshotOnColdStartFailure() {
        List<Quote> emergency = List.of(
                Quote.builder().code("blue").name("Blue Snapshot").buy(1500.0).sell(1520.0).currency("USD").updatedAt(Instant.now()).build()
        );

        when(cacheService.getQuotes()).thenReturn(Optional.empty());
        when(dolarApiClient.fetchQuotes()).thenThrow(new UpstreamServiceException("Connection timeout", 504));
        when(cacheService.getStaleQuotes()).thenReturn(Optional.empty());
        when(cacheService.getEmergencySnapshot()).thenReturn(emergency);

        QuoteResult result = quoteService.getAllQuotes();

        assertThat(result.getCacheStatus()).isEqualTo("STALE_FALLBACK");
        assertThat(result.getQuotes()).hasSize(1);
        assertThat(result.getQuotes().getFirst().getName()).isEqualTo("Blue Snapshot");
    }

    @Test
    @DisplayName("Should transition Circuit Breaker to OPEN after repeated failures and reject upstream calls")
    void shouldOpenCircuitBreakerAfterThresholdFailures() {
        when(cacheService.getQuotes()).thenReturn(Optional.empty());
        when(dolarApiClient.fetchQuotes()).thenThrow(new UpstreamServiceException("Service Down", 503));
        when(cacheService.getStaleQuotes()).thenReturn(Optional.of(mockQuotes));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("dollarApi");

        // Trigger failures to exceed minimumNumberOfCalls (2) and failureRateThreshold (50%)
        quoteService.getAllQuotes();
        quoteService.getAllQuotes();

        // Circuit should transition to OPEN
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Third call should immediately trigger fallback via CallNotPermittedException
        QuoteResult fallbackResult = quoteService.getAllQuotes();
        assertThat(fallbackResult.getCacheStatus()).isEqualTo("STALE_FALLBACK");

        // Verify that after opening, additional calls were fast-failed by circuit breaker
        assertThat(cb.getMetrics().getNumberOfNotPermittedCalls()).isGreaterThanOrEqualTo(1);
    }
}
