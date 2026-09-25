package com.dollarapi.exception;

import lombok.Getter;

@Getter
public class UpstreamServiceException extends RuntimeException {
    private final int statusCode;

    public UpstreamServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public UpstreamServiceException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
