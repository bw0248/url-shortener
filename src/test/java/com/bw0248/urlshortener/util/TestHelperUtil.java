package com.bw0248.urlshortener.util;

import com.bw0248.urlshortener.mapping.UrlMapping;

public class TestHelperUtil {
    public static UrlMapping exampleMapping() {
        return new UrlMapping("example.com", "abc");
    }
}
