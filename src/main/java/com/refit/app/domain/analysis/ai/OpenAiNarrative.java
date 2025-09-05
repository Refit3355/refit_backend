package com.refit.app.domain.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenAiNarrative {

    private final ChatClient chat;
    private final ObjectMapper om = new ObjectMapper();

    // 마지막 JSON 객체만 안전 추출
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}\\s*$");

    public OpenAiNarrative(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    // ===== 프롬프트 =====

    // (C) 화장품: 요약 + 3개 그룹 오버뷰
    private static final String SYSTEM_COSMETIC = """
            OUTPUT: JSON ONLY with keys:
            {"summary":"string","risky_overview":"string","caution_overview":"string","safe_overview":"string"}
            Rules:
            - Korean, clear and practical.
            - summary: 2–3 sentences. Overall safety, notable risky/caution, helpful safe benefits, simple tips, balanced recommendation for %s.
            - Each *_overview: 2–3 sentences about the whole group.
            - If empty: "해당되는 성분은 없습니다."
            - End: "개인 피부 상태에 따라 차이가 있을 수 있습니다."
            """;

    // (FAST) 리스트만 받아서 빠르게 분류 (요약/오버뷰 없음)
    private static final String SYSTEM_CLASSIFY_LIST_FAST = """
            TASK:
            - Classify each ingredient name into exactly one of {final_safe, final_caution, final_risky}
              according to EWG hazard propensity (low=SAFE, medium=CAUTION, high=RISKY).
            - If unsure, prefer "final_caution".
            - Do NOT invent names. Use only provided names.
            OUTPUT: JSON ONLY -> {"final_safe":["..."],"final_caution":["..."],"final_risky":["..."]}
            Rules:
            - Korean output only. No extra text outside JSON.
            """;

    // (옵션) 영양제 관련 기존 것들 필요시 유지
    private static final String SYSTEM_SUPPLEMENT_TWO_BLOCKS = """
            OUTPUT: JSON ONLY -> {"benefits":"string","condition_cautions":"string"}
            Rules:
            - Korean only, based ONLY on OCR_TEXT.
            - benefits: 2–3 sentences (purpose, actives, claims).
            - condition_cautions: 2–3 sentences if OCR mentions conditions/diseases/medications; else default.
            - Exclude dosage, price, maker.
            """;

    /* ========================= 영양제 두 문단 ========================= */
    public SupplementTwoBlocks buildSupplementTwoBlocksFromText(String ocrText) {
        String user = """
                OCR_TEXT:
                ---
                %s
                ---
                Return JSON only with keys: benefits, condition_cautions.
                """.formatted(ocrText == null ? "" : ocrText);

        try {
            String raw = chat.prompt()
                    .system(SYSTEM_SUPPLEMENT_TWO_BLOCKS)
                    .user(u -> u.text(user))
                    .options(OpenAiChatOptions.builder()
                            .maxCompletionTokens(330)
                            .temperature(0.0)
                            .build())
                    .call()
                    .content();


            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);
            String benefits = root.path("benefits").asText("요약 생성에 실패했어요.");
            String cautions = root.path("condition_cautions").asText("특이한 주의사항이 명시되어 있지 않습니다.");
            return new SupplementTwoBlocks(benefits, cautions);
        } catch (Exception e) {
            return new SupplementTwoBlocks("요약 생성에 실패했어요.", "특이한 주의사항이 명시되어 있지 않습니다.");
        }
    }

    /* ========================= 빠른 분류: 이름 리스트만 ========================= */
    public ClassificationResult classifyListFast(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ClassificationResult(List.of(), List.of(), List.of());
        }

        final int MAX_NAMES = 40;
        List<String> limited = names.size() > MAX_NAMES ? names.subList(0, MAX_NAMES) : names;

        String user = """
                NAMES:
                %s
                Return JSON only with keys: final_safe, final_caution, final_risky.
                """.formatted(String.join(", ", limited));

        try {
            String raw = chat.prompt()
                    .system(SYSTEM_CLASSIFY_LIST_FAST)
                    .user(u -> u.text(user))
                    .options(OpenAiChatOptions.builder()
                            .maxCompletionTokens(230)
                            .temperature(0.0)
                            .build())
                    .call()
                    .content();


            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);

            List<String> outSafe    = readArrayAsList(root, "final_safe");
            List<String> outCaution = readArrayAsList(root, "final_caution");
            List<String> outRisky   = readArrayAsList(root, "final_risky");

            return new ClassificationResult(outSafe, outCaution, outRisky);
        } catch (Exception e) {
            log.warn("[Narrative.classifyListFast] parse error: {}", e.toString());
            return new ClassificationResult(List.of(), names, List.of());
        }
    }

    /* ========================= 내러티브(짧게) ========================= */
    public CosmeticNarrative buildCosmeticNarrative(
            List<String> danger, List<String> caution, List<String> safe,
            String memberName, int matchRate) {

        String system = SYSTEM_COSMETIC.formatted(memberName);

        int N = 3;
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
                    .options(OpenAiChatOptions.builder()
                            .maxCompletionTokens(330)
                            .temperature(0.0)
                            .build())
                    .call()
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

    // -------------------- helpers --------------------
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

    // -------------------- DTOs --------------------
    public record ClassificationResult(List<String> safe, List<String> caution,
                                       List<String> risky) {}

    public record CosmeticNarrative(String summary, String riskyText, String cautionText,
                                    String safeText) {}

    public record SupplementTwoBlocks(String benefits, String conditionCautions) {}
}
