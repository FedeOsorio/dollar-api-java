package com.dollarapi.client;

import com.dollarapi.exception.UpstreamServiceException;
import com.dollarapi.model.ChaosMode;
import com.dollarapi.model.DolarApiResponse;
import com.dollarapi.model.Quote;
import com.dollarapi.service.ChaosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DolarApiClient {

    private final RestClient restClient;
    private final ChaosService chaosService;

    @Value("${dollar.upstream.url:https://dolarapi.com/v1/dolares}")
    private String upstreamUrl;

    public List<Quote> fetchQuotes() {
        simulateChaosIfActive();

        log.info("🌐 [UPSTREAM] Fetching latest dollar quotes from {}", upstreamUrl);

        try {
            List<DolarApiResponse> response = restClient.get()
                    .uri(upstreamUrl)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("❌ [UPSTREAM] Received HTTP {} from external API", res.getStatusCode());
                        throw new UpstreamServiceException("Upstream API returned " + res.getStatusCode(), res.getStatusCode().value());
                    })
                    .body(new ParameterizedTypeReference<List<DolarApiResponse>>() {});

            if (response == null || response.isEmpty()) {
                throw new UpstreamServiceException("Upstream returned empty body", HttpStatus.BAD_GATEWAY.value());
            }

            return mapToQuotes(response);
        } catch (UpstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [UPSTREAM] Call failed: {}", e.getMessage());
            throw new UpstreamServiceException("Failed to reach upstream provider: " + e.getMessage(), e, HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    private void simulateChaosIfActive() {
        ChaosMode mode = chaosService.getMode();
        switch (mode) {
            case OUTAGE -> {
                log.warn("🧪 [CHAOS ACTIVE] Simulating 503 Service Unavailable outage");
                throw new UpstreamServiceException("Simulated upstream outage (Chaos Mode: OUTAGE)", HttpStatus.SERVICE_UNAVAILABLE.value());
            }
            case TIMEOUT -> {
                log.warn("🧪 [CHAOS ACTIVE] Simulating upstream TIMEOUT (Sleeping 3500ms > 2000ms limit)");
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                throw new UpstreamServiceException("Simulated upstream timeout (Chaos Mode: TIMEOUT)", HttpStatus.GATEWAY_TIMEOUT.value());
            }
            case SLOW -> {
                log.warn("🧪 [CHAOS ACTIVE] Simulating high latency (Sleeping 1500ms)");
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            case NONE -> {
                // Proceed normally
            }
        }
    }

    private List<Quote> mapToQuotes(List<DolarApiResponse> rawQuotes) {
        List<Quote> quotes = new ArrayList<>();
        for (DolarApiResponse raw : rawQuotes) {
            String code = normalizeCode(raw.getCasa());
            Instant updatedAt = raw.getFechaActualizacion() != null ? raw.getFechaActualizacion() : Instant.now();

            quotes.add(Quote.builder()
                    .code(code)
                    .name(raw.getNombre() != null ? raw.getNombre() : code)
                    .buy(raw.getCompra())
                    .sell(raw.getVenta())
                    .currency(raw.getMoneda() != null ? raw.getMoneda() : "USD")
                    .updatedAt(updatedAt)
                    .build());
        }
        return quotes;
    }

    private String normalizeCode(String casa) {
        if (casa == null) return "unknown";
        String lower = casa.trim().toLowerCase();
        return switch (lower) {
            case "bolsa" -> "mep";
            case "contadoconliqui" -> "ccl";
            default -> lower;
        };
    }
}
