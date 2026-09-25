package com.dollarapi.service;

import com.dollarapi.exception.QuoteNotFoundException;
import com.dollarapi.model.Quote;
import com.dollarapi.model.QuoteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final CacheService cacheService;
    private final ResilientQuoteFetcher resilientQuoteFetcher;

    @Override
    public QuoteResult getAllQuotes() {
        Optional<List<Quote>> cached = cacheService.getQuotes();
        if (cached.isPresent()) {
            log.info("⚡ [CACHE HIT] Returning {} quotes from cache", cached.get().size());
            return new QuoteResult(cached.get(), "HIT");
        }

        log.info("🔎 [CACHE MISS] Fetching fresh quotes through resilience layer");
        return resilientQuoteFetcher.fetchUpstream();
    }

    @Override
    public QuoteResult getQuoteByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new QuoteNotFoundException("blank");
        }

        QuoteResult allQuotesResult = getAllQuotes();
        String normalizedSearchCode = normalizeSearchCode(code);

        Quote matchedQuote = allQuotesResult.getQuotes().stream()
                .filter(q -> matchQuoteCode(q.getCode(), normalizedSearchCode))
                .findFirst()
                .orElseThrow(() -> new QuoteNotFoundException(code));

        return new QuoteResult(List.of(matchedQuote), allQuotesResult.getCacheStatus(), allQuotesResult.getMessage());
    }

    private String normalizeSearchCode(String input) {
        String lower = input.trim().toLowerCase();
        return switch (lower) {
            case "bolsa" -> "mep";
            case "contadoconliqui" -> "ccl";
            default -> lower;
        };
    }

    private boolean matchQuoteCode(String actualCode, String normalizedSearchCode) {
        if (actualCode == null) return false;
        String actualLower = actualCode.trim().toLowerCase();
        if (actualLower.equals(normalizedSearchCode)) {
            return true;
        }
        if (normalizedSearchCode.equals("mep") && actualLower.equals("bolsa")) return true;
        if (normalizedSearchCode.equals("ccl") && actualLower.equals("contadoconliqui")) return true;
        return false;
    }
}
