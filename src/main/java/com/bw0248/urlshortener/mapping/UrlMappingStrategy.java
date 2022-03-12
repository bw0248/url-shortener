package com.bw0248.urlshortener.mapping;

@FunctionalInterface
public interface UrlMappingStrategy {
    String map(final String url, final long sequence);
}
