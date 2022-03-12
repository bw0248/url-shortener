package com.bw0248.urlshortener.service;

import com.bw0248.urlshortener.ShortenerService;
import com.bw0248.urlshortener.config.SequenceMappingConfig;
import com.bw0248.urlshortener.config.ServiceConfig;
import com.bw0248.urlshortener.exception.StorageException;
import com.bw0248.urlshortener.mapping.SequenceMappingStrategy;
import com.bw0248.urlshortener.storage.mongo.MongoUrlStorage;
import com.bw0248.urlshortener.util.TestHelperUtil;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ServiceTest {
    // see configuration below -> ShortenerService is initialized with mockStorage as member instead of actual MongoStorage
    @Autowired private MongoUrlStorage mockStorage;
    @Autowired ShortenerService service;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(mockStorage);
    }

    @Test
    void testStorageErrorRetryPolicy() {
        val exampleMapping = TestHelperUtil.exampleMapping();
        // store successfully without retries
        when(mockStorage.insertMapping(any())).thenReturn(Optional.of(exampleMapping));
        val successfullyStored = service.shorten(exampleMapping.getLongUrl());
        assertTrue(successfullyStored.isPresent());
        assertEquals(exampleMapping.getShortUrl(), successfullyStored.get().getShortUrl());

        // max out on tries - nothing stored, Optional::empty returned
        Mockito.reset(mockStorage);
        when(mockStorage.insertMapping(any())).thenReturn(Optional.empty());//thenThrow(RuntimeException.class);
        val failedShorten = service.shorten(exampleMapping.getLongUrl());
        assertFalse(failedShorten.isPresent());

        // fail on first two tries, succeed on third try
        Mockito.reset(mockStorage);
        when(mockStorage.insertMapping(any()))
                .thenThrow(StorageException.class)
                .thenThrow(StorageException.class)
                .thenReturn(Optional.of(exampleMapping));
        val successfulShorten = service.shorten(exampleMapping.getLongUrl());
        assertTrue(successfulShorten.isPresent());
        assertEquals(exampleMapping.getShortUrl(), successfulShorten.get().getShortUrl());
    }

    @Test
    void testCaching() {
        val exampleMapping = TestHelperUtil.exampleMapping();

        // let mocked storage return example mapping when invoked with example::shortUrl
        when(mockStorage.findMappingByShortUrl(exampleMapping.getShortUrl()))
                .thenReturn(Optional.of(exampleMapping));

        // successfully retrieve long url for the first time
        assertEquals(
                Optional.of(exampleMapping.getLongUrl()),
                service.getMappedLongUrl(exampleMapping.getShortUrl())
        );

        // expect cache to contain example mapping now
        assertNotNull(cacheManager.getCache("mappings"));
        val cachedMapping = Optional.ofNullable(cacheManager.getCache("mappings"))
                        .map(e -> e.get("abc", String.class));
        assertTrue(cachedMapping.isPresent());
        assertEquals(exampleMapping.getLongUrl(), cachedMapping.get());

        // make some more requests
        val numRequests = 20;
        IntStream.range(0, numRequests)
                .boxed()
                .map(i -> service.getMappedLongUrl(exampleMapping.getShortUrl()))
                .forEach(longUrl -> assertEquals(exampleMapping.getLongUrl(), longUrl.get()));

        // verify mocked store was only invoked once in total
        verify(mockStorage, Mockito.times(1))
                .findMappingByShortUrl(exampleMapping.getShortUrl());
    }

    @EnableCaching
    @TestConfiguration
    public static class CachingTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("mappings");
        }
        @Bean
        @Primary
        public MongoUrlStorage mockStorage() {
            return Mockito.mock(MongoUrlStorage.class);
        }

        @Bean
        @Primary
        public ShortenerService shortenerService() {
            val serviceConfig = new ServiceConfig();
            serviceConfig.setMaxRetries(3);
            return new ShortenerService(
                    mockStorage(),
                    new SequenceMappingStrategy(SequenceMappingConfig.base4()),
                    serviceConfig
            );
        }
    }
}
