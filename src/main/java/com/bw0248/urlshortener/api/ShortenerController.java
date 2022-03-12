package com.bw0248.urlshortener.api;

import com.bw0248.urlshortener.ShortenerService;
import com.bw0248.urlshortener.api.dto.ShortenRequest;
import com.bw0248.urlshortener.api.dto.ShortenResponse;
import com.bw0248.urlshortener.mapping.UrlMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

@RestController
@Slf4j
public class ShortenerController {
    private final ShortenerService service;

    @Autowired
    public ShortenerController(
            final ShortenerService service) {
        this.service = service;
    }

    // TODO: should only be allowed for admin
    @GetMapping("/api/health")
    @ResponseStatus(HttpStatus.OK)
    public void healthCheck() {}

    // TODO: should only be allowed for admin
    @GetMapping("/api/all")
    public List<UrlMapping> getAllMappings() {
        return service.getAllMappings();
    }

    @PostMapping("/api/shorten")
    public ShortenResponse createMapping(@Valid @RequestBody final ShortenRequest shortenRequest) {
        if (!isValidUrl(shortenRequest.getUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Url supplied");
        }
        return service.shorten(shortenRequest.getUrl().strip())
                .map(ShortenResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Could not shorten " + shortenRequest.getUrl())
                );
    }

    @GetMapping("/{shortUrl}")
    public void resolveShortUrl(HttpServletResponse response, @PathVariable @NotBlank final String shortUrl) {
        try {
            response.sendRedirect(
                    service.getMappedLongUrl(shortUrl)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ShortUrl not found"))
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not resolve shortUrl");
        }
    }

    private boolean isValidUrl(final String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            log.debug("Recognized invalid url {}", url);
        }
        return false;
    }
}
