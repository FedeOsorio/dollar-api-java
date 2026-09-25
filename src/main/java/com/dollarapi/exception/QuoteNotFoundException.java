package com.dollarapi.exception;

public class QuoteNotFoundException extends RuntimeException {
    public QuoteNotFoundException(String code) {
        super("Quote not found for code: " + code);
    }
}
