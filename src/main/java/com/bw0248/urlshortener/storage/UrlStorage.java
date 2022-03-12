package com.bw0248.urlshortener.storage;

import com.bw0248.urlshortener.exception.DuplicateShortUrlException;
import com.bw0248.urlshortener.exception.StorageException;
import com.bw0248.urlshortener.mapping.UrlMapping;

import java.util.List;
import java.util.Optional;

public interface UrlStorage {
    List<UrlMapping> findAllMappings();
    void deleteAllMappings();
    long mappingsCount();
    long nextUniqueId();
    Optional<UrlMapping> insertMapping(final UrlMapping mapping) throws DuplicateShortUrlException, StorageException;
    Optional<UrlMapping> findMappingByShortUrl(final String shortUrl);
}
