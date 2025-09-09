package com.refit.app.infra.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "clova.ocr")
public class ClovaOcrProps {
    private String endpoint;
    private String secret;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private Integer writeTimeoutMs;
    private Integer totalTimeoutMs;
}
