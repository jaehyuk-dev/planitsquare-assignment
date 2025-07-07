package com.planitsquare.assignment_jaehyuk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${external.api.nager.base-url}")
    private String baseUrl;

    @Value("${external.api.nager.max-in-memory-size}")
    private int maxInMemorySize;

    @Bean
    public WebClient nagerDateWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();
    }
}
