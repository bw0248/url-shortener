package com.bw0248.urlshortener.storage.mongo;

import com.bw0248.urlshortener.mapping.UrlMapping;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("UrlMappings")
@Data
@Builder
public class UrlMappingDocument {
    // note: indices are automatically created by spring (see application.yml)
    @Id private final String id;
    @NonNull private final String longUrl;
    @NonNull @Indexed(unique = true) private final String shortUrl;     // shortUrl has a unique index to prevent duplicates
    @NonNull private final Instant createdAt;

    public static UrlMappingDocument from(final UrlMapping mapping) {
        return UrlMappingDocument.builder()
                .longUrl(mapping.getLongUrl())
                .shortUrl(mapping.getShortUrl())
                .createdAt(Instant.now())
                .build();
    }
}
