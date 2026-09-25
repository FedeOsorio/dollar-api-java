package com.dollarapi.service;

import com.dollarapi.model.Quote;
import com.dollarapi.model.QuoteResult;

public interface QuoteService {
    QuoteResult getAllQuotes();
    QuoteResult getQuoteByCode(String code);
}
