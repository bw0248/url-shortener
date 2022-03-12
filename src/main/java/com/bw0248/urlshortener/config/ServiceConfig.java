package com.bw0248.urlshortener.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class ServiceConfig {
    @Value("${service.max-retries}")
    private int maxRetries;
}
