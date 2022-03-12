package com.bw0248.urlshortener.api;

import com.bw0248.urlshortener.ShortenerService;
import com.bw0248.urlshortener.api.dto.ShortenRequest;
import com.bw0248.urlshortener.util.TestHelperUtil;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ShortenerControllerTest {
    @Mock private ShortenerService service;
    @InjectMocks private ShortenerController controller;

    @Test
    void testUrlValidation() {
        val exampleMapping = TestHelperUtil.exampleMapping();
        when(service.shorten(any())).thenReturn(Optional.of(exampleMapping));
        val validUrls = List.of(
                "https://example.com",
                "http://example.com",
                "https://www.example.com",
                "http://www.example.com"
        );

        // expect no exception for valid urls
        validUrls.stream()
                .map(url -> ShortenRequest.builder().url(url).build())
                .forEach(controller::createMapping);

        val invalidUrls = List.of(
                "https//www.example.com",
                "example",
                "",
                "https://",
                "ttps://example.com"
        );

        // expect failed request for each invalid url
        invalidUrls.stream()
                .map(url -> ShortenRequest.builder().url(url).build())
                .forEach(this::invalidShortenRequest);
    }

    private void invalidShortenRequest(ShortenRequest shortenRequest) {
        val exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.createMapping(shortenRequest)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
