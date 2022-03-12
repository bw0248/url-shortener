package com.bw0248.urlshortener.storage.mongo;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *   Sequence id to be used generating unique short urls.
 *   To guarantee uniqueness:
 *          * key is unique so there can never be multiple entries of 'counter'
 *          * @Version for optimistic locking -> makes concurrent writes fail in case of version mismatch
 */
@Document("Sequence")
@Data
@Builder
@Jacksonized
public class SequenceDocument {
    @Transient public static final String COLLECTION = "Sequence";
    @Transient public static final String KEY = "sequence";
    @Transient public static final String ID = "id";
    @Id private String objId;
    @Indexed(unique = true) private String key = KEY;
    private long id;
    @Version long version;

    public static SequenceDocument init() {
        return SequenceDocument
                .builder()
                .key(KEY)
                .id(0)
                .build();
    }
}
