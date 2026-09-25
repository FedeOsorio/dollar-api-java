package com.dollarapi.service;

import com.dollarapi.model.Quote;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class CacheService {

    private static final String REDIS_KEY_LATEST = "dollar:quotes:latest";
    private static final String REDIS_KEY_STALE = "dollar:quotes:stale";

    @Value("${dollar.cache.ttl-seconds:300}")
    private long cacheTtlSeconds;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // In-Memory Fallback Tier
    private volatile List<Quote> inMemoryLatestQuotes = null;
    private volatile Instant inMemoryExpiry = Instant.MIN;
    private volatile List<Quote> inMemoryStaleQuotes = null;

    private final AtomicBoolean redisOperational = new AtomicBoolean(true);

    public CacheService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                        ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<Quote>> getQuotes() {
        if (redisTemplate != null && redisOperational.get()) {
            try {
                Object cached = redisTemplate.opsForValue().get(REDIS_KEY_LATEST);
                if (cached != null) {
                    List<Quote> quotes = convertToQuoteList(cached);
                    if (quotes != null && !quotes.isEmpty()) {
                        return Optional.of(quotes);
                    }
                }
            } catch (Exception e) {
                log.warn("[CACHE] Redis get operation failed ({}), falling back to in-memory cache", e.getMessage());
                redisOperational.set(false);
            }
        }

        if (inMemoryLatestQuotes != null && Instant.now().isBefore(inMemoryExpiry)) {
            return Optional.of(inMemoryLatestQuotes);
        }

        return Optional.empty();
    }

    public void saveQuotes(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }

        this.inMemoryLatestQuotes = new ArrayList<>(quotes);
        this.inMemoryExpiry = Instant.now().plusSeconds(cacheTtlSeconds);
        this.inMemoryStaleQuotes = new ArrayList<>(quotes);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_KEY_LATEST, quotes, Duration.ofSeconds(cacheTtlSeconds));
                redisTemplate.opsForValue().set(REDIS_KEY_STALE, quotes);
                if (!redisOperational.get()) {
                    log.info("[CACHE] Redis connection restored!");
                    redisOperational.set(true);
                }
            } catch (Exception e) {
                log.warn("[CACHE] Redis save operation failed ({}), cached in-memory only", e.getMessage());
                redisOperational.set(false);
            }
        }
    }

    public Optional<List<Quote>> getStaleQuotes() {
        if (redisTemplate != null && redisOperational.get()) {
            try {
                Object stale = redisTemplate.opsForValue().get(REDIS_KEY_STALE);
                if (stale != null) {
                    List<Quote> quotes = convertToQuoteList(stale);
                    if (quotes != null && !quotes.isEmpty()) {
                        return Optional.of(quotes);
                    }
                }
            } catch (Exception e) {
                log.warn("[CACHE] Redis getStale operation failed ({}), checking in-memory stale cache", e.getMessage());
                redisOperational.set(false);
            }
        }

        if (inMemoryStaleQuotes != null && !inMemoryStaleQuotes.isEmpty()) {
            return Optional.of(inMemoryStaleQuotes);
        }

        return Optional.empty();
    }

    public List<Quote> getEmergencySnapshot() {
        Instant now = Instant.now();
        return List.of(
                Quote.builder().code("oficial").name("Oficial").buy(1490.0).sell(1540.0).currency("USD").updatedAt(now).build(),
                Quote.builder().code("blue").name("Blue").buy(1540.0).sell(1560.0).currency("USD").updatedAt(now).build(),
                Quote.builder().code("mep").name("Bolsa (MEP)").buy(1539.0).sell(1545.6).currency("USD").updatedAt(now).build(),
                Quote.builder().code("ccl").name("Contado con liquidación").buy(1612.5).sell(1614.3).currency("USD").updatedAt(now).build(),
                Quote.builder().code("tarjeta").name("Tarjeta").buy(1937.0).sell(2002.0).currency("USD").updatedAt(now).build(),
                Quote.builder().code("cripto").name("Cripto").buy(1605.68).sell(1610.04).currency("USD").updatedAt(now).build(),
                Quote.builder().code("mayorista").name("Mayorista").buy(1510.0).sell(1519.0).currency("USD").updatedAt(now).build()
        );
    }

    public boolean isRedisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            var connection = redisTemplate.getConnectionFactory() != null ? redisTemplate.getConnectionFactory().getConnection() : null;
            if (connection != null) {
                String ping = connection.ping();
                connection.close();
                boolean ok = "PONG".equalsIgnoreCase(ping);
                redisOperational.set(ok);
                return ok;
            }
        } catch (Exception e) {
            redisOperational.set(false);
        }
        return false;
    }

    public String getCacheType() {
        return (redisTemplate != null && redisOperational.get()) ? "REDIS" : "IN_MEMORY";
    }

    @SuppressWarnings("unchecked")
    private List<Quote> convertToQuoteList(Object cached) {
        try {
            if (cached instanceof List<?> list) {
                return objectMapper.convertValue(list, new TypeReference<List<Quote>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to deserialize quotes from cache: {}", e.getMessage());
        }
        return null;
    }
}
