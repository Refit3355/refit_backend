package com.refit.app.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class TossClientConfig {
    private final Environment env;

    @Bean("tossWebClient")
    public WebClient tossWebClient() {
        String secret = env.getProperty("toss.secret-key");
        return WebClient.builder()
                .baseUrl(env.getProperty("toss.api-base"))
                .defaultHeaders(h -> h.setBasicAuth(secret, ""))
                .build();
    }
}

