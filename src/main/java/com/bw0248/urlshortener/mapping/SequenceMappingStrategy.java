package com.bw0248.urlshortener.mapping;

import com.bw0248.urlshortener.config.SequenceMappingConfig;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  Inspired by: https://stackoverflow.com/a/742047/11120993
 *  Unique DB sequence id is translated to base n where n is supplied by SequenceMappingConfig.
 *  The calculated base n value is subsequently used as an index into the base n alphabet supplied by SequenceMappingConfig
 *  Max possible mappings considering base n and max word length of l = n ** l
 *  Examples:
 *      * base36 with l = 8 -> 2.8e12 unique mappings
 *      * base62 with l = 8 -> 2.1e14 unique mappings
 *
 *  Example for base 2 with alphabet {a,b} and at most 2 chars per word
 *  Expectation of mapping from Base 10 int to binary to string:
 *          0 ->  0 -> "a"
 *          1 ->  1 -> "b"
 *          2 -> 10 -> "ba"
 *          3 -> 11 -> "bb"
 *
 *  Afterwards the possible mapping range is exhausted.
 */
@Component(value = "SequenceMapping")
public class SequenceMappingStrategy implements UrlMappingStrategy {
    private final SequenceMappingConfig config;

    @Autowired
    public SequenceMappingStrategy(final SequenceMappingConfig config) {
        this.config = config;
    }

    @Override
    public String map(final String original , final long sequenceId) {
        if (sequenceId == 0) {
            return String.valueOf(config.getAlphabet().charAt(0));
        }

        StringBuilder builder = new StringBuilder();
        var number = sequenceId;
        while (number > 0) {
            val remainder = number % config.getBase();
            builder.append(config.getAlphabet().charAt((int) remainder));
            number = number / config.getBase();
        }

        return builder.reverse().toString();
    }
}
