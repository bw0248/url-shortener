package com.bw0248.urlshortener.exception;

public class DuplicateShortUrlException extends RuntimeException {
    public DuplicateShortUrlException(final String message, final Exception e) {
        super(message, e);
    }
}
