package com.refit.app.infra.ocr;

import com.refit.app.infra.image.ImagePreprocessor;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaOcrProvider implements OcrProvider {

    private final WebClient ocrWebClient;
    private final ClovaOcrProps props;

    @Override
    public String fullText(byte[] imageBytes, String filename, String contentType) {
        long t0 = System.nanoTime();

        // 1) 이미지 전처리(JPEG 재인코딩 + 리사이즈 등)
        boolean isPdf = contentType != null && contentType.toLowerCase().contains("pdf");
        byte[] prepped = isPdf ? imageBytes : ImagePreprocessor.preprocess(imageBytes);
        String format = isPdf ? "pdf" : "jpg"; // 전처리했으면 무조건 jpg

        String base64 = Base64.getEncoder().encodeToString(prepped);

        Map<String, Object> payload = Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", format,
                        "name", (filename == null ? "upload" : filename),
                        "data", base64
                ))
        );

        String endpoint = props.getEndpoint() == null ? "" : props.getEndpoint().trim();

        // 2) retrieve + onStatus: 비Deprecated & 에러바디 로깅
        ClovaResp resp = ocrWebClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-OCR-SECRET", props.getSecret())
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> {
                                    int code = r.statusCode().value(); // ← rawStatusCode() 대체
                                    log.warn("[ClovaOCR][{}] {} -> {}", code, endpoint, body);
                                    return Mono.error(new RuntimeException(
                                            "Clova OCR " + code + ": " + body));
                                })
                )
                .bodyToMono(ClovaResp.class)
                .timeout(Duration.ofMillis(
                        props.getTotalTimeoutMs() != null ? props.getTotalTimeoutMs() : 15000))
                .block();

        long t1 = System.nanoTime();
        log.info("[ClovaOCR] in={}KB -> prepped={}KB, latency={} ms",
                kb(imageBytes.length), kb(prepped.length), (t1 - t0) / 1_000_000);

        if (resp == null || resp.images() == null || resp.images().isEmpty()) {
            return "";
        }

        var img0 = resp.images().get(0);
        if (img0 == null || img0.fields() == null || img0.fields().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (var f : img0.fields()) {
            if (f != null && f.inferText() != null) {
                sb.append(f.inferText()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static int kb(int bytes) {
        return Math.max(1, bytes / 1024);
    }

    public record ClovaResp(List<Image> images) {

        public record Image(List<Field> fields) {

        }

        public record Field(String inferText) {

        }
    }
}
