package com.bw0248.urlshortener.mapping;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UrlMapping {
    private final String longUrl;
    private final String shortUrl;
}
