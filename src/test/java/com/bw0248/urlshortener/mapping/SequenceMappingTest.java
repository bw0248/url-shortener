package com.bw0248.urlshortener.mapping;

import com.bw0248.urlshortener.config.SequenceMappingConfig;
import com.bw0248.urlshortener.mapping.SequenceMappingStrategy;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
public class SequenceMappingTest {
    private final static String url = "https://example.com";    // not important for this test

    /**
     *  Mapping alphabet {a,b} with at most 2 chars per word
     *  Expectation of mapping from Base 10 int to binary to string:
     *          0 ->  0 -> "a"
     *          1 ->  1 -> "b"
     *          2 -> 10 -> "ba"
     *          3 -> 11 -> "bb"
     */
    @Test
    void testBase2Mapping() {
        val mapped = mapExhaustive(
                SequenceMappingConfig.withAlphabet("ab"),
                2
        );

        val expectedAlphabet = List.of("a", "b", "ba", "bb");
        assertEquals(expectedAlphabet.size(), mapped.size());
        assertThat(mapped).containsExactlyInAnyOrderElementsOf(expectedAlphabet);
    }

    @Test
    void testBase4Mapping() {
        mapExhaustive(SequenceMappingConfig.base4(), 8);
    }

    @Test
    void testBase8Mapping() {
        mapExhaustive(SequenceMappingConfig.base8(), 8);
    }

    private MappingSet<String> mapExhaustive(final SequenceMappingConfig config, int maxCharsPerMapping) {
        val strategy = new SequenceMappingStrategy(config);
        long maxPossibleMappings = (long) Math.pow(config.getBase(), maxCharsPerMapping);

        // make sure there are no duplicate mappings - MappingSet throws if not able to insert
        MappingSet<String> mappings = LongStream.range(0, maxPossibleMappings)
                .boxed()
                .map(i -> strategy.map(url, i))
                .collect(Collectors.toCollection(MappingSet::new));

        // make sure mapping range was actually exhausted
        assertEquals(maxPossibleMappings, mappings.size());

        return mappings;
    }

    private static class MappingSet<String> extends HashSet<String> {
        @Override
        public boolean add(String mapping) {
            if(!super.add(mapping)) {
                throw new IllegalArgumentException("Duplicate Mapping is not allowed");
            }
            return true;
        }
    }
}
