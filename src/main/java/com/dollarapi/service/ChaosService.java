package com.dollarapi.service;

import com.dollarapi.model.ChaosMode;
import com.dollarapi.model.ChaosResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ChaosService {

    private final AtomicReference<ChaosMode> currentMode = new AtomicReference<>(ChaosMode.NONE);

    public ChaosMode getMode() {
        return currentMode.get();
    }

    public ChaosResponse setMode(ChaosMode mode) {
        ChaosMode previous = currentMode.getAndSet(mode);
        log.warn("🧪 [CHAOS ENGINE] Transitioned simulation mode: {} -> {}", previous, mode);

        String message = switch (mode) {
            case NONE -> "Normal operation resumed. Upstream requests proceed as usual.";
            case OUTAGE -> "Chaos OUTAGE enabled. External provider will immediately fail with HTTP 503.";
            case TIMEOUT -> "Chaos TIMEOUT enabled. External provider calls will exceed the 2s timeout.";
            case SLOW -> "Chaos SLOW enabled. External provider calls will experience high latency (1500ms).";
        };

        return ChaosResponse.builder()
                .status("SUCCESS")
                .message(message)
                .currentMode(mode)
                .timestamp(Instant.now())
                .build();
    }
}
