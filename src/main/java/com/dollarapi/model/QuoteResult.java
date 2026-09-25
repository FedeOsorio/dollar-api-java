package com.dollarapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResult {
    private List<Quote> quotes;
    private String cacheStatus; // "HIT", "MISS", "STALE_FALLBACK"
    private String message;

    public QuoteResult(List<Quote> quotes, String cacheStatus) {
        this.quotes = quotes;
        this.cacheStatus = cacheStatus;
        this.message = null;
    }
}
