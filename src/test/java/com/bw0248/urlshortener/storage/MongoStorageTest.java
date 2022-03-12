package com.bw0248.urlshortener.storage;

import com.bw0248.urlshortener.storage.mongo.MongoUrlStorage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class MongoStorageTest {
    @Autowired private MongoUrlStorage mongoStorage;

    /**
     * Test concurrent access - this is non-deterministic, so it's not perfect but a start
     * @throws InterruptedException
     */
    @Test
    void testSequenceId() throws InterruptedException {
        val numTestRuns = 20;
        for (int i = 0; i < numTestRuns; i++) {
            runSequenceIdTest();
        }
    }

    private void runSequenceIdTest() throws InterruptedException {
        Set<Long> retrievedIds = ConcurrentHashMap.newKeySet();
        val firstId = mongoStorage.nextUniqueId();
        retrievedIds.add(firstId);
        int numThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        for (int i = 0 ; i < numThreads; i++) {
            executorService.execute(() -> retrieveIdTask(latch, retrievedIds));
        }
        latch.await();
        assertEquals(retrievedIds.size(), new HashSet<>(retrievedIds).size()); // assert uniqueness
        assertEquals(numThreads + 1, retrievedIds.size());
        for (long i = firstId; i < numThreads; i++) {
            assertTrue(retrievedIds.contains(i));
        }
    }

    private void retrieveIdTask(final CountDownLatch latch, final Set<Long> ids) {
        val nextId = mongoStorage.nextUniqueId();
        val res = ids.add(nextId);
        assertTrue(res, "Not able to insert retrieved sequence id into result set");
        latch.countDown();
    }
}
