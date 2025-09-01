package com.refit.app.domain.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class OpenAiNarrative {

    private final ChatClient chat;
    private final ObjectMapper om = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}\\s*$");

    public OpenAiNarrative(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    /* ========================= 영양제 요약(개인화 X) ========================= */
    public String buildSupplementSummaryFromText(String ocrText) {
        String system = """
                You are a Korean nutrition label explainer.
                OUTPUT: JSON ONLY -> { "summary": "string" }
                Write in Korean. Use ONLY the provided OCR text (no external knowledge). No personalization.
                Focus on efficacy/benefits ONLY.
                - 4–6 concise sentences covering: what kind of supplement it is, key actives/nutrients seen in the text and what they help with, major claims/target benefits, and explicit cautions/allergens if present.
                - Do NOT include dosage/serving size/how to take, schedules, storage, price, contact info, manufacturer/distributor details, or customer service instructions.
                - If the OCR text is insufficient, briefly say that information is insufficient.
                - End with: "개인 체질 및 복용 환경에 따라 차이가 있을 수 있습니다."
                """;

        String user = """
                OCR_TEXT:
                ---
                %s
                ---
                Return JSON only.
                """.formatted(ocrText == null ? "" : ocrText);

        try {
            String raw = chat.prompt().system(system).user(u -> u.text(user)).call().content();
            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);
            return root.path("summary").asText("요약 생성에 실패했어요.");
        } catch (Exception e) {
            return "요약 생성에 실패했어요.";
        }
    }


    /* ========================= 화장품: 전체 요약 + 카테고리별 쉬운 글 ========================= */
    public CosmeticNarrative buildCosmeticNarrative(
            List<String> danger, List<String> caution, List<String> safe,
            String memberName, int matchRate
    ) {
        String system = """
                You are a domain expert writing Korean consumer-friendly cosmetic analysis.
                STRICT OUTPUT: JSON ONLY with exactly:
                {
                  "summary": "string",
                  "risky_overview": "string",
                  "caution_overview": "string",
                  "safe_overview": "string"
                }
                Style:
                - Korean, clear and practical.
                - "summary": 5–9 sentences. Cover overall safety impression, notable risky/caution items (if any) and why,
                  beneficial/safe items (if any) and effects, simple usage tips (patch test, frequency, layering),
                  and a balanced recommendation for %s.
                - Each of risky/caution/safe overviews: 2–4 short sentences in simple words
                  summarizing the whole group (NOT per-ingredient bullets).
                - If a group is empty, say briefly: "해당되는 성분은 없습니다."
                - Final sentence of summary: "개인 피부 상태에 따라 차이가 있을 수 있습니다."
                """.formatted(memberName);

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
            String raw = chat.prompt().system(system).user(u -> u.text(user)).call().content();
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

    /* ============ (선택) DB 미등록 성분을 LLM으로 3분류 ============ */
    public ClassificationResult classifyUnknowns(List<String> candidates, String productTypeKr) {
        if (candidates == null || candidates.isEmpty()) {
            return new ClassificationResult(List.of(), List.of(), List.of());
        }

        String system = """
                You are a Korean cosmetic ingredients classifier.
                STRICT OUTPUT: JSON ONLY with exactly:
                { "safe": ["..."], "caution": ["..."], "risky": ["..."] }
                Rules:
                - Consider typical facial skincare usage for general consumers.
                - Input list CANDIDATES contains the only allowed ingredient names.
                - Classify each candidate into exactly one bucket (safe/caution/risky).
                - Output MUST be a subset of CANDIDATES; do not add or modify names.
                - If uncertain about an item, prefer "caution".
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
            String raw = chat.prompt().system(system).user(u -> u.text(user)).call().content();
            String json = stripToJson(raw);
            JsonNode root = om.readTree(json);

            List<String> safe = readArrayAsList(root, "safe");
            List<String> caution = readArrayAsList(root, "caution");
            List<String> risky = readArrayAsList(root, "risky");

            // 안전장치: 원본 후보에 없는 건 제거
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
