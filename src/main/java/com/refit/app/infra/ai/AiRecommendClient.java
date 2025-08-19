package com.refit.app.infra.ai;

import com.refit.app.domain.product.dto.request.AiRecommendRequest;
import com.refit.app.domain.product.dto.response.AiRecommendResponse;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class AiRecommendClient {

    @Value("${external.ai.base-url}")
    private String baseUrl;

    @Value("${external.ai.path}")
    private String path;

    @Value("${external.ai.timeout-ms:20000}")
    private int timeoutMs;

    private RestTemplate restTemplate() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setResponseTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }

    public AiRecommendResponse request(AiRecommendRequest req) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AiRecommendRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<AiRecommendResponse> resp =
                restTemplate().postForEntity(uri, entity, AiRecommendResponse.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("AI API 호출 실패: " + resp.getStatusCode());
        }
        return resp.getBody();
    }
}
