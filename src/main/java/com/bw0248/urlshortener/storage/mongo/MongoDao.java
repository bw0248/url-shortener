package com.bw0248.urlshortener.storage.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoDao extends MongoRepository<UrlMappingDocument, String> {
    @Query(value="{shortUrl:'?0'}")
    UrlMappingDocument findMappingByShortUrl(final String shortUrl);

    @Query(value="{longUrl:'?0'}")
    UrlMappingDocument findMappingByLongUrl(final String longUrl);
}
