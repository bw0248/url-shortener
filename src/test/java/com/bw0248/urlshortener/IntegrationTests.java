package com.bw0248.urlshortener;

import com.bw0248.urlshortener.api.dto.ShortenResponse;
import com.bw0248.urlshortener.storage.UrlStorage;
import com.bw0248.urlshortener.util.UrlMappingList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationTests {
	private static final String GET_ALL_PATH = "/api/all";
	private static final String SHORTEN_PATH = "/api/shorten";
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper mapper;
	@Autowired private UrlStorage storage;

	@BeforeEach
	public void beforeEach() {
		storage.deleteAllMappings();
		assertEquals(0, storage.mappingsCount());
	}

	@Test
	void testSimpleShortenRequest() {
		val mappingsBeforeRequest = getAllMappings().size();
		makeShortenRequest();
		assertEquals(mappingsBeforeRequest + 1, getAllMappings().size());
	}

	/**
	 * Test concurrent access - this is non-deterministic, so it's not perfect but a start
	 * @throws InterruptedException
	 */
	@Test
	void testConcurrentRequests() throws InterruptedException {
		int numThreads = 20;
		Set<String> shortenedUrls = ConcurrentHashMap.newKeySet();
		val mappingsBefore = getAllMappings().size();
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		CountDownLatch latch = new CountDownLatch(numThreads);
		for (int i = 0 ; i < numThreads; i++) {
			executorService.execute(() -> makeConcurrentShortenRequest(shortenedUrls, latch));
		}
		latch.await();
		assertEquals(numThreads, shortenedUrls.size());
		assertEquals(mappingsBefore + numThreads, getAllMappings().size());
	}

	private void makeConcurrentShortenRequest(
			final Set<String> shortenedUrls,
			final CountDownLatch latch) {
		val response = makeShortenRequest();
		val res = shortenedUrls.add(response.getShortUrl());
		assertTrue(res, "Not able to insert retrieved short url into result set");
		latch.countDown();
	}

	private ShortenResponse makeShortenRequest() {
		val shortenResponse = shorten(Map.of("url", "https://example.com"));
		assertNotNull(shortenResponse);
		assertNotNull(shortenResponse.getShortUrl());
		assertNotNull(shortenResponse.getLongUrl());
		return shortenResponse;
	}

	private ShortenResponse shorten(final Map<String, String> payload) {
		try {
			val shortenResult = mockMvc
					.perform(post(SHORTEN_PATH)
									 .contentType("application/json")
									 .content(mapper.writeValueAsString(payload)))
					.andExpect(status().isOk());
			return deserializeResponse(shortenResult.andReturn(), ShortenResponse.class);
		} catch (Exception e) {
			throw new IllegalStateException("POST " + SHORTEN_PATH + " failed");
		}
	}

	private UrlMappingList getAllMappings() {
		try {
			val result = mockMvc.perform(get(GET_ALL_PATH)).andExpect(status().isOk());
			return deserializeResponse(result.andReturn(), UrlMappingList.class);
		} catch (Exception e) {
			throw new IllegalStateException("GET " + GET_ALL_PATH + " failed");
		}
	}

	private <T> T deserializeResponse(final MvcResult result, Class<T> type) {
		try {
			val rawString = result.getResponse().getContentAsString();
			return mapper.readValue(rawString, type);
		} catch (UnsupportedEncodingException | JsonProcessingException e) {
			throw new IllegalStateException("could not deserialize response");
		}
	}
}
