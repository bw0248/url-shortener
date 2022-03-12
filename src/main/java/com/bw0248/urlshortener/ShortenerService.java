package com.bw0248.urlshortener;

import com.bw0248.urlshortener.config.ServiceConfig;
import com.bw0248.urlshortener.exception.StorageException;
import com.bw0248.urlshortener.mapping.UrlMapping;
import com.bw0248.urlshortener.mapping.UrlMappingStrategy;
import com.bw0248.urlshortener.storage.UrlStorage;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ShortenerService {
    private final UrlStorage storage;
    private final UrlMappingStrategy mappingStrategy;
    private final ServiceConfig config;

    @Autowired
    public ShortenerService(
            final UrlStorage storage,
            @Qualifier("SequenceMapping") final UrlMappingStrategy mappingStrategy,
            final ServiceConfig config) {
        this.storage = storage;
        this.mappingStrategy = mappingStrategy;
        this.config = config;
    }

    public List<UrlMapping> getAllMappings() {
        return storage.findAllMappings();
    }

    /**
     * Shorten supplied url with configured mapping strategy and insert into storage.
     * Potential StorageExceptions are handled according to config::maxRetries.
     *
     * For possible future implementations of mapping strategies that might produce collisions (e.g. randomized mapping)
     * an additional policy for DuplicateKeyErrors can be implemented.
     *
     * @param url
     * @return Optional of UrlMapping in case of success, otherwise Optional::empty
     */
    public Optional<UrlMapping> shorten(@NonNull final String url) {
        // in case of a general storage error e.g. DB not reachable
        RetryPolicy<Optional<UrlMapping>> storageErrorPolicy = RetryPolicy
                .<Optional<UrlMapping>>builder()
                .withMaxRetries(config.getMaxRetries())
                .handle(StorageException.class)
                .withDelay(Duration.ofMillis(100))
                .onFailedAttempt(e -> log.warn("storage error when inserting UrlMapping"))
                .onRetriesExceeded(e -> log.error("not able to insert UrlMapping into storage"))
                .build();

        val sequenceId = storage.nextUniqueId();
        return Failsafe
                .with(storageErrorPolicy)
                .get(() -> insertIntoStorage(url, sequenceId));
    }

    /**
     * Retrieve corresponding longUrl from storage for supplied shortUrl
     * Mappings are cached in a basic Spring-Boot cache for now which defaults to a ConcurrentHashMap with max entries of 32
     *
     * @param shortUrl
     * @return Optional containing corresponding longUrl if present
     */
    @Cacheable("mappings")
    public Optional<String> getMappedLongUrl(final String shortUrl) {
        return storage.findMappingByShortUrl(shortUrl)
                .map(UrlMapping::getLongUrl);
    }

    private Optional<UrlMapping> insertIntoStorage(final String longUrl, final long sequenceId) {
        val shortUrl = mappingStrategy.map(longUrl, sequenceId);
        return storage.insertMapping(new UrlMapping(longUrl, shortUrl));
    }
}
