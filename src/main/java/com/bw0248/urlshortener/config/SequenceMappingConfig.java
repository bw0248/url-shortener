package com.bw0248.urlshortener.config;

import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


/**
 *  Max number of mappings considering 8 char limit: 36 ** 8 -> roughly 2.8e12
 *  One advantage of base36 is all characters can be same case -> less error-prone for users when typing
 */
@Configuration
@Getter
public class SequenceMappingConfig {
    // @TODO: possibly sanitize alphabet to prevent unfortunate mappings
    private static final String ALLOWED_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
    private static final String BASE_36_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String BASE_8_ALPHABET = "abcdefgh";
    private static final String BASE_4_ALPHABET = "abcd";

    private final String alphabet;

    // injected via application.yml
    public SequenceMappingConfig(@Value("${sequence-mapping.alphabet}") final String alphabet) {
        validateAlphabet(alphabet);
        this.alphabet = alphabet;
    }

    public int getBase() {
        return alphabet.length();
    }

    public static SequenceMappingConfig base4() {
        return new SequenceMappingConfig(BASE_4_ALPHABET);
    }

    public static SequenceMappingConfig base8() {
        return new SequenceMappingConfig(BASE_8_ALPHABET);
    }

    public static SequenceMappingConfig base36() {
        return new SequenceMappingConfig(BASE_36_ALPHABET);
    }

    public static SequenceMappingConfig withAlphabet(final String alphabet) {
        validateAlphabet(alphabet);
        return new SequenceMappingConfig(alphabet);
    }

    private static void validateAlphabet(final String alphabet) {
        if (alphabet.length() > ALLOWED_CHARACTERS.length()) {
            throw new IllegalArgumentException("alphabet cannot be larger than " + ALLOWED_CHARACTERS.length() + " characters");
        }

        // validate only valid characters present in supplied alphabet
        val illegalChar = alphabet.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .filter(c -> !ALLOWED_CHARACTERS.contains(c))
                .findAny();

        if (illegalChar.isPresent()) {
            throw new IllegalArgumentException(illegalChar.get() + " is not allowed in alphabet");
        }
    }
}
