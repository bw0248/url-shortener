package com.bw0248.urlshortener.api.dto;

import com.bw0248.urlshortener.mapping.UrlMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenResponse {
    private String longUrl;
    private String shortUrl;

    public static ShortenResponse from(final UrlMapping mapping) {
        return new ShortenResponse(mapping.getLongUrl(), mapping.getShortUrl());
    }
}
