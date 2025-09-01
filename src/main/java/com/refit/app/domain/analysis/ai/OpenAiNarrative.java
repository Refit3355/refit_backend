package com.refit.app.domain.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@Component
public class OpenAiNarrative {

    private final ChatClient chat;
    private final ObjectMapper om = new ObjectMapper();

    // 마지막 JSON 객체만 안전 추출
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}\\s*$");

    // ===== 프롬프트 (static final: 프롬프트 캐시 히트 ↑) =====

    // (A) 영양제: 이미지 → 원샷 요약
    private static final String SYSTEM_SUPPLEMENT_ONESHOT = """
            You analyze Korean dietary supplement labels from an image.
            OUTPUT: JSON ONLY -> {"summary":"string"}
            Rules:
            - Korean. Use ONLY text visible in the image. No external info/personalization.
            - Focus benefits/efficacy only.
            - 3–4 concise sentences: type of supplement, key actives/nutrients & their benefits,
              major claims/target benefits, explicit cautions/allergens if present.
            - EXCLUDE dosage/how-to/schedule/storage/price/contacts/manufacturer/CS.
            - If insufficient text, say so briefly.
            - End with: "개인 체질 및 복용 환경에 따라 차이가 있을 수 있습니다."
            """;

    // (B) 영양제: OCR 텍스트가 이미 있을 때(백업 경로)
    private static final String SYSTEM_SUPPLEMENT_TEXT = """
            You explain Korean nutrition labels.
            OUTPUT: JSON ONLY -> {"summary":"string"}
            Rules:
            - Korean. Use ONLY the provided OCR text. No external info/personalization.
            - Focus benefits/efficacy only.
            - 3–4 concise sentences: type, key actives & effects, claims/target benefits, explicit cautions/allergens (if any).
            - EXCLUDE dose/how-to/schedule/storage/price/contacts/maker/CS.
            - If insufficient, say so briefly.
            - End with: "개인 체질 및 복용 환경에 따라 차이가 있을 수 있습니다."
            """;

    // (C) 화장품 내러티브 (요약 + 3개 그룹 오버뷰)
    private static final String SYSTEM_COSMETIC = """
            You write Korean consumer-friendly cosmetic analysis.
            OUTPUT: JSON ONLY with keys:
            {"summary":"string","risky_overview":"string","caution_overview":"string","safe_overview":"string"}
            Style/Rules:
            - Korean, clear and practical.
            - summary: 3–4 sentences. Overall safety, notable risky/caution (if any) and why,
              beneficial/safe (if any) and effects, simple tips (patch test/frequency/layering),
              balanced recommendation for %s.
            - Each *_overview: 2–4 short sentences about the whole group (NOT per-ingredient bullets).
            - If a group is empty: "해당되는 성분은 없습니다."
            - End of summary: "개인 피부 상태에 따라 차이가 있을 수 있습니다."
            """;

    public OpenAiNarrative(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    /* ========================= 영양제: 이미지 → 원샷 요약 ========================= */
    public String buildSupplementSummaryFromImage(byte[] imageBytes,
            @Nullable String filename,
            @Nullable String contentType) {
        try {
            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(
                            (contentType == null || contentType.isBlank()) ? "image/jpeg"
                                    : contentType))
                    .data(imageBytes)
                    .build();

            String raw = chat.prompt()
                    .system(SYSTEM_SUPPLEMENT_ONESHOT)
                    .user(u -> u.text("Return JSON only.").media(media))
                    .call()     // 동기 호출 (안정)
                    .content();

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);
            return root.path("summary").asText("요약 생성에 실패했어요.");
        } catch (Exception e) {
            return "요약 생성에 실패했어요.";
        }
    }

    /* ========================= 영양제: OCR 텍스트 → 요약 ========================= */
    public String buildSupplementSummaryFromText(String ocrText) {
        String user = """
                OCR_TEXT:
                ---
                %s
                ---
                Return JSON only.
                """.formatted(ocrText == null ? "" : ocrText);

        try {
            String raw = chat.prompt()
                    .system(SYSTEM_SUPPLEMENT_TEXT)
                    .user(u -> u.text(user))
                    .call()     // 동기 호출로 단순화
                    .content();

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);
            return root.path("summary").asText("요약 생성에 실패했어요.");
        } catch (Exception e) {
            return "요약 생성에 실패했어요.";
        }
    }

    /* ========================= 화장품: 전체 요약 + 3개 오버뷰 ========================= */
    public CosmeticNarrative buildCosmeticNarrative(
            List<String> danger, List<String> caution, List<String> safe,
            String memberName, int matchRate
    ) {
        String system = SYSTEM_COSMETIC.formatted(memberName);

        int N = 8;
        String user = """
                Context:
                - User nickname: %s
                - Match rate (0-100): %d
                - RISKY (sample up to %d, total=%d): %s
                - CAUTION (sample up to %d, total=%d): %s
                - SAFE   (sample up to %d, total=%d): %s
                Return JSON only with keys: summary, risky_overview, caution_overview, safe_overview.
                """.formatted(
                memberName, matchRate,
                N, danger.size(), previewList(danger, N),
                N, caution.size(), previewList(caution, N),
                N, safe.size(), previewList(safe, N)
        );

        try {
            String raw = chat.prompt()
                    .system(system)
                    .user(u -> u.text(user))
                    .call()     // 동기 호출로 단순/안정
                    .content();

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);

            String summary = root.path("summary").asText("요약 생성에 실패했어요.");
            String risky = root.path("risky_overview").asText("해당되는 성분은 없습니다.");
            String cautionTxt = root.path("caution_overview").asText("해당되는 성분은 없습니다.");
            String safeTxt = root.path("safe_overview").asText("해당되는 성분은 없습니다.");

            return new CosmeticNarrative(summary, risky, cautionTxt, safeTxt);
        } catch (Exception e) {
            return new CosmeticNarrative(
                    "요약 생성에 실패했어요.",
                    "해당되는 성분은 없습니다.",
                    "해당되는 성분은 없습니다.",
                    "해당되는 성분은 없습니다."
            );
        }
    }

    /* ============ DB 미등록 성분을 3분류 ============ */
    public ClassificationResult classifyUnknowns(List<String> candidates, String productTypeKr) {
        if (candidates == null || candidates.isEmpty()) {
            return new ClassificationResult(List.of(), List.of(), List.of());
        }

        String system = """
                You are a Korean cosmetic ingredients classifier.
                OUTPUT: JSON ONLY -> {"safe":["..."],"caution":["..."],"risky":["..."]}
                Rules:
                - Typical facial skincare context.
                - Input list CANDIDATES is the only allowed names.
                - Each candidate -> exactly one bucket (safe/caution/risky).
                - Output MUST be a subset of CANDIDATES; do not add/modify names.
                - If uncertain, prefer "caution".
                - No explanations. JSON only.
                """;

        String user = """
                Product type hint: %s
                CANDIDATES: %s
                Return JSON only with keys: safe, caution, risky.
                """.formatted(
                (productTypeKr == null ? "cosmetic" : productTypeKr),
                candidates.toString()
        );

        try {
            String raw = chat.prompt()
                    .system(system)
                    .user(u -> u.text(user))
                    .call()
                    .content();

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);

            List<String> safe = readArrayAsList(root, "safe");
            List<String> caution = readArrayAsList(root, "caution");
            List<String> risky = readArrayAsList(root, "risky");

            // 원본 후보에 없는 건 제거(안전장치)
            safe.retainAll(candidates);
            caution.retainAll(candidates);
            risky.retainAll(candidates);

            return new ClassificationResult(safe, caution, risky);
        } catch (Exception e) {
            return new ClassificationResult(List.of(), List.of(), List.of());
        }
    }

    /* -------------------- helpers -------------------- */
    private static List<String> readArrayAsList(JsonNode root, String key) {
        List<String> out = new ArrayList<>();
        if (root != null && root.has(key) && root.get(key).isArray()) {
            for (JsonNode n : root.get(key)) {
                if (n.isTextual()) {
                    out.add(n.asText());
                }
            }
        }
        return out;
    }

    private static String previewList(List<String> xs, int n) {
        if (xs == null || xs.isEmpty()) {
            return "[]";
        }
        int cut = Math.min(n, xs.size());
        return xs.subList(0, cut).toString();
    }

    private static String stripToJson(String s) {
        if (s == null) {
            return "";
        }
        Matcher m = JSON_BLOCK.matcher(s.trim());
        if (m.find()) {
            return m.group();
        }
        return s.replaceAll("^```(?:json)?\\s*", "")
                .replaceAll("\\s*```\\s*$", "")
                .trim();
    }

    /* -------------------- DTOs -------------------- */
    public record ClassificationResult(List<String> safe, List<String> caution,
                                       List<String> risky) {

    }

    public record CosmeticNarrative(String summary, String riskyText, String cautionText,
                                    String safeText) {

    }
}
