package com.refit.app.infra.ocr;

import com.refit.app.infra.image.ImagePreprocessor;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaOcrProvider implements OcrProvider {

    private final WebClient ocrWebClient;
    private final ClovaOcrProps props;

    @Override
    public String fullText(byte[] imageBytes, String filename, String contentType) {
        long t0 = System.nanoTime();

        byte[] prepped = ImagePreprocessor.preprocess(imageBytes);

        String base64 = Base64.getEncoder().encodeToString(prepped);
        Map<String, Object> payload = Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", "jpg",                              // ★ 중요: 무조건 jpg
                        "name", (filename == null ? "upload.jpg" : filename),
                        "data", base64                                // 또는 "url": "<presigned-url>"
                ))
        );

        var mono = ocrWebClient.post()
                .uri(props.getEndpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-OCR-SECRET", props.getSecret())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ClovaResp.class);

        long totalMs = props.getTotalTimeoutMs() == null ? 15000 : props.getTotalTimeoutMs();
        ClovaResp resp = mono.timeout(Duration.ofMillis(totalMs)).block();

        long t1 = System.nanoTime();
        log.info("[ClovaOCR] in={}KB -> prepped={}KB, latency={} ms",
                kb(imageBytes.length), kb(prepped.length), (t1 - t0) / 1_000_000);

        if (resp == null || resp.images() == null || resp.images().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (var f : resp.images().get(0).fields()) {
            if (f.inferText() != null) sb.append(f.inferText()).append("\n");
        }
        return sb.toString().trim();
    }

    private static int kb(int bytes) { return Math.max(1, bytes / 1024); }

    public record ClovaResp(List<Image> images) {
        public record Image(List<Field> fields) {}
        public record Field(String inferText) {}
    }
}
