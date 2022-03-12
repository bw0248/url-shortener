package com.bw0248.urlshortener.exception;

public class StorageException extends RuntimeException {
    public StorageException(final String message, final Exception e) {
        super(message, e);
    }
}
