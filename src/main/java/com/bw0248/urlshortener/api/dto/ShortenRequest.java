package com.bw0248.urlshortener.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@Jacksonized
public class ShortenRequest {
    @NotBlank(message = "url may not be empty")
    private String url;
}
