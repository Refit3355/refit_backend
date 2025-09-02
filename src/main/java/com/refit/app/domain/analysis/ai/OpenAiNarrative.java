package com.refit.app.domain.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

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

    // (A) 영양제: 이미지 → 원샷 요약
    private static final String SYSTEM_SUPPLEMENT_ONESHOT = """
            OUTPUT: JSON ONLY -> {"summary":"string"}
            Rules:
            - Korean only, based ONLY on visible image text.
            - 3–4 sentences: type, key actives & benefits, claims, cautions.
            - Exclude dosage, price, maker.
            - If insufficient, say so.
            - End with: "개인 체질 및 복용 환경에 따라 차이가 있을 수 있습니다."
            """;

    // (B) 영양제: OCR 텍스트 → 단일 요약
    private static final String SYSTEM_SUPPLEMENT_TEXT = """
            OUTPUT: JSON ONLY -> {"summary":"string"}
            Rules:
            - Korean only, based ONLY on provided OCR text.
            - 3–4 sentences: type, actives & effects, claims, cautions.
            - Exclude dose, price, maker.
            - If insufficient, say so.
            - End with: "개인 체질 및 복용 환경에 따라 차이가 있을 수 있습니다."
            """;

    // (C) 화장품: 요약 + 3개 그룹 오버뷰
    private static final String SYSTEM_COSMETIC = """
            OUTPUT: JSON ONLY with keys:
            {"summary":"string","risky_overview":"string","caution_overview":"string","safe_overview":"string"}
            Rules:
            - Korean, clear and practical.
            - summary: 3–4 sentences. Overall safety, notable risky/caution, helpful safe benefits, simple tips, balanced recommendation for %s.
            - Each *_overview: 2–4 sentences about the whole group.
            - If empty: "해당되는 성분은 없습니다."
            - End: "개인 피부 상태에 따라 차이가 있을 수 있습니다."
            """;

    // (D) 영양제: 두 문단(효능 + 질병 관련 주의)
    private static final String SYSTEM_SUPPLEMENT_TWO_BLOCKS = """
            OUTPUT: JSON ONLY -> {"benefits":"string","condition_cautions":"string"}
            Rules:
            - Korean only, based ONLY on OCR_TEXT.
            - benefits: 3–4 sentences (purpose, actives, claims).
            - condition_cautions: 2–4 sentences if OCR mentions conditions/diseases/medications; else default.
            - Exclude dosage, price, maker.
            """;

    // (E) 화장품: unknown 분류 + 내러티브 (EWG 기준)
    private static final String SYSTEM_COSMETIC_CLASSIFY_AND_NARRATE = """
            OUTPUT: JSON ONLY with keys:
            {"final_safe":["..."],"final_caution":["..."],"final_risky":["..."],
             "summary":"string","risky_overview":"string","caution_overview":"string","safe_overview":"string"}
            Rules:
            - Korean only. No extra text outside JSON.
            - UNKNOWN candidates: classify each based on **EWG hazard rating** into exactly one of {safe,caution,risky}; if unsure, prefer "caution".
            - Do NOT add names not in provided lists.
            - Narratives: summary 3–4 sentences; *_overview 2–3 sentences about whole group.
            - If empty: "해당되는 성분은 없습니다."
            - End: "개인 피부 상태에 따라 차이가 있을 수 있습니다."
            """;


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

            //속도 확인
            long t0 = System.nanoTime();

            String raw = chat.prompt()
                    .system(SYSTEM_SUPPLEMENT_ONESHOT)
                    .user(u -> u.text("Return JSON only.").media(media))
                    .call()
                    .content();

            //속도 확인
            long t1 = System.nanoTime();
            log.info("[Narrative.suppTwoBlocks] latency={} ms", (t1 - t0) / 1_000_000);

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);
            return root.path("summary").asText("요약 생성에 실패했어요.");
        } catch (Exception e) {
            return "요약 생성에 실패했어요.";
        }
    }

    /* ========================= 영양제: OCR 텍스트 → 단일 요약 ========================= */
    public String buildSupplementSummaryFromText(String ocrText) {
        String user = """
                OCR_TEXT:
                ---
                %s
                ---
                Return JSON only.
                """.formatted(ocrText == null ? "" : ocrText);

        //속도 확인
        long t0 = System.nanoTime();

        try {
            String raw = chat.prompt()
                    .system(SYSTEM_SUPPLEMENT_TEXT)
                    .user(u -> u.text(user))
                    .call()
                    .content();

            //속도 확인
            long t1 = System.nanoTime();
            log.info("[Narrative.suppTwoBlocks] latency={} ms", (t1 - t0) / 1_000_000);

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);
            return root.path("summary").asText("요약 생성에 실패했어요.");
        } catch (Exception e) {
            return "요약 생성에 실패했어요.";
        }
    }

    /* ========================= 영양제: OCR 텍스트 → 두 문단 ========================= */
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

    /* ========================= 화장품: 기존(단일 호출) 내러티브 ========================= */
    public CosmeticNarrative buildCosmeticNarrative(
            List<String> danger, List<String> caution, List<String> safe,
            String memberName, int matchRate) {

        String system = SYSTEM_COSMETIC.formatted(memberName);

        int N = 10;
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

    /**
     * (유지) 외부에서 병렬 버전처럼 부르고 싶을 때의 진입점. 현재는 단일 호출로 처리하므로 기존 로직을 재사용.
     */
    public CosmeticNarrative buildCosmeticNarrativeParallel(
            List<String> danger, List<String> caution, List<String> safe,
            String memberName, int matchRate) {
        return buildCosmeticNarrative(danger, caution, safe, memberName, matchRate);
    }

    /* ========================= 화장품: unknown 분류 + 내러티브 (단일 호출) ========================= */
    public CosmeticClassifyAndNarrative classifyAndNarrate(
            List<String> danger, List<String> caution, List<String> safe,
            List<String> unknown, String memberName, int matchRate) {

        // 기존 시스템 프롬프트 + 발명 금지/입력만 사용 규칙 강화
        String system = SYSTEM_COSMETIC_CLASSIFY_AND_NARRATE + """
                - Classify ONLY names present in the provided lists; do NOT invent or alter names.
                - final_* MUST be subsets of the union of SAFE, CAUTION, RISKY, UNKNOWN inputs.
                - If unsure about a name, prefer "caution".
                """;

        // 미리보기 샘플 수(토큰 절감용)
        final int PREVIEW_N = 12;

        // 🟢 Speed Mode: unknown 상한 K (토큰/속도 최적화)
        //    품질 모드로 전량 보내고 싶으면 K = Integer.MAX_VALUE 로 바꾸면 됨.
        final int K = 6;
        List<String> unknownSlice = (unknown == null) ? List.of()
                : unknown.subList(0, Math.min(K, unknown.size()));

        String user = """
                Context:
                - User nickname: %s
                - Match rate (0-100): %d
                - SAFE   (sample up to %d, total=%d): %s
                - CAUTION(sample up to %d, total=%d): %s
                - RISKY  (sample up to %d, total=%d): %s
                - UNKNOWN(cap %d, total=%d): %s
                Return JSON only with keys: final_safe, final_caution, final_risky, summary, risky_overview, caution_overview, safe_overview.
                """.formatted(
                memberName, matchRate,
                PREVIEW_N, safe.size(), previewList(safe, PREVIEW_N),
                PREVIEW_N, caution.size(), previewList(caution, PREVIEW_N),
                PREVIEW_N, danger.size(), previewList(danger, PREVIEW_N),
                unknownSlice.size(), unknown.size(), previewList(unknownSlice, unknownSlice.size())
        );

        try {
            String raw = chat.prompt()
                    .system(system)
                    .user(u -> u.text(user))
                    .call()
                    .content();

            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);

            List<String> outSafe = readArrayAsList(root, "final_safe");
            List<String> outCaution = readArrayAsList(root, "final_caution");
            List<String> outRisky = readArrayAsList(root, "final_risky");

            // LLM이 입력에 없던 이름을 섞지 못하게 제한
            var allowed = new java.util.HashSet<String>();
            if (safe != null) {
                allowed.addAll(safe);
            }
            if (caution != null) {
                allowed.addAll(caution);
            }
            if (danger != null) {
                allowed.addAll(danger);
            }
            allowed.addAll(unknownSlice);

            outSafe.retainAll(allowed);
            outCaution.retainAll(allowed);
            outRisky.retainAll(allowed);

            String summary = root.path("summary").asText("요약 생성에 실패했어요.");
            String riskyTxt = root.path("risky_overview").asText("해당되는 성분은 없습니다.");
            String cautionTxt = root.path("caution_overview").asText("해당되는 성분은 없습니다.");
            String safeTxt = root.path("safe_overview").asText("해당되는 성분은 없습니다.");

            return new CosmeticClassifyAndNarrative(
                    outSafe, outCaution, outRisky,
                    new CosmeticNarrative(summary, riskyTxt, cautionTxt, safeTxt)
            );
        } catch (Exception e) {
            return new CosmeticClassifyAndNarrative(
                    safe, caution, danger,
                    new CosmeticNarrative(
                            "요약 생성에 실패했어요.",
                            "해당되는 성분은 없습니다.",
                            "해당되는 성분은 없습니다.",
                            "해당되는 성분은 없습니다."
                    )
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
                                       List<String> risky) {

    }

    public record CosmeticNarrative(String summary, String riskyText, String cautionText,
                                    String safeText) {

    }

    public record SupplementTwoBlocks(String benefits, String conditionCautions) {

    }

    public record CosmeticClassifyAndNarrative(
            List<String> finalSafe, List<String> finalCaution, List<String> finalRisky,
            CosmeticNarrative narrative) {

    }
}
