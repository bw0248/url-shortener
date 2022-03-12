package com.bw0248.urlshortener.storage.mongo;

import com.bw0248.urlshortener.exception.DuplicateShortUrlException;
import com.bw0248.urlshortener.exception.StorageException;
import com.bw0248.urlshortener.mapping.UrlMapping;
import com.bw0248.urlshortener.storage.UrlStorage;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MongoUrlStorage implements UrlStorage {
    private final MongoDao dao;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public MongoUrlStorage(final MongoDao dao, final MongoTemplate mongoTemplate) {
        this.dao = dao;
        this.mongoTemplate = mongoTemplate;
        this.mongoTemplate.setWriteConcern(WriteConcern.ACKNOWLEDGED);
        mongoTemplate.setWriteResultChecking(WriteResultChecking.EXCEPTION);
        if (mongoTemplate.getCollection(SequenceDocument.COLLECTION).countDocuments() == 0) {
            log.info("initializing sequence counter");
            mongoTemplate.insert(SequenceDocument.init());
        }
    }

    @Override
    public List<UrlMapping> findAllMappings() {
        return dao.findAll()
                .stream()
                .map(this::toUrlMapping)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllMappings() {
        dao.deleteAll();
    }

    @Override
    public long mappingsCount() {
        return dao.count();
    }

    @Override
    public Optional<UrlMapping> insertMapping(final UrlMapping mapping) throws DuplicateShortUrlException, StorageException {
        val mappingDocument = UrlMappingDocument.from(mapping);
        try {
            return Optional
                    .of(mongoTemplate.insert(mappingDocument))
                    .map(this::toUrlMapping);
        } catch (DuplicateKeyException e) {
            throw new DuplicateShortUrlException(
                    "Cannot insert UrlMapping - short url " + mapping.getShortUrl() + " already exists", e
            );
        } catch (MongoException e) {
            val msg = "Writing to database failed";
            log.error(msg, e);
            throw new StorageException(msg, e);
        }
    }

    @Override
    public Optional<UrlMapping> findMappingByShortUrl(String shortUrl) {
        log.debug("Cache miss for {}", shortUrl);
        return Optional
                .ofNullable(dao.findMappingByShortUrl(shortUrl))
                .map(this::toUrlMapping);
    }

    @Override
    public long nextUniqueId() {
        return upsertCounter()
                .map(SequenceDocument::getId)
                .orElseThrow(() -> {
                    val msg = "Unable to upsert counter";
                    log.error(msg);
                    throw new IllegalStateException(msg);
                });
    }

    // atomically select and increment sequence id
    private Optional<SequenceDocument> upsertCounter() {
        return Optional.ofNullable(
                mongoTemplate.findAndModify(
                       new Query(Criteria.where("key").is(SequenceDocument.KEY)),
                       new Update().inc(SequenceDocument.ID, 1),
                       FindAndModifyOptions.options().upsert(true).returnNew(false),
                       SequenceDocument.class)
        );
    }

    private UrlMapping toUrlMapping(final UrlMappingDocument document) {
        return new UrlMapping(document.getLongUrl(), document.getShortUrl());
    }
}
