package com.refit.app.domain.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.infra.image.ImagePreprocessor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@Slf4j
@Component
public class OpenAiVisionOcr {

    private final ChatClient chat;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiVisionOcr(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}\\s*$");

    // 라벨/영양 관련 키워드
    private static final Set<String> NUTRITION_HEADER = Set.of(
            "영양정보", "영양성분", "영양성분표", "1일 영양성분 기준치", "1일 영양성분 기준치에 대한 비율", "열량", "kcal",
            "nutrition facts"
    );
    private static final Set<String> MACROS = Set.of(
            "나트륨", "탄수화물", "당류", "지방", "단백질", "콜레스테롤", "포화지방", "트랜스지방",
            "sodium", "carbohydrate", "sugars", "fat", "protein", "cholesterol", "saturated fat",
            "trans fat"
    );
    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+)?\\s*(?:mg|㎎|g|µg|mcg|iu|%)\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> STOPWORDS = Set.of(
            "전성분", "성분", "원재료", "원재료명", "성분표", "주의사항", "사용법", "보관방법", "섭취방법",
            "주의", "경고", "알레르기", "부작용", "브랜드", "제조사"
    );

    /**
     * FAST: 전성분/성분 블록 텍스트만 추출 (초간단 스키마)
     */
    public String extractIngredientBlockFast(byte[] imageBytes, @Nullable String filename,
            @Nullable String contentType) {
        String system = """
                TASK:
                - Find the ingredients section (전성분/성분/Ingredients) in the image.
                - Return JSON ONLY -> {"block":"string"} where block is the raw text of that section.
                - If not clearly found, return {"block":""}.
                Rules:
                - No guessing, no markdown, keep line breaks.
                """;
        String user = "Return JSON only.";

        Media media = toMedia(imageBytes, filename, contentType);

        long t0 = System.nanoTime();
        String rsp = chat.prompt()
                .system(system)
                .user(u -> u.text(user).media(media))
                .call()
                .content();
        long t1 = System.nanoTime();
        log.info("[OCR.extractFast] latency={} ms", (t1 - t0) / 1_000_000);

        String json = stripToJson(rsp);
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("block").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 정식 추출(기존)
     */
    public ExtractedIngredients extract(byte[] imageBytes, @Nullable String filename,
            @Nullable String contentType) {
        String system = """
                TASK:
                - Extract ingredients from image’s “Ingredients” section (전성분/성분).
                - If not found, return found=false and empty list.
                - Do NOT guess or invent content.
                OUTPUT: JSON ONLY.
                RULES:
                - Each ingredient must appear in `raw_block`.
                - Include `name`, `confidence` (0–1), `evidence`.
                - Exclude confidence < 0.80, units (mg, g, %, IU), marketing, brands, numbers.
                - Split on common delimiters and deduplicate.
                SCHEMA:
                {
                  "found": true|false,
                  "section": "전성분|성분|none",
                  "raw_block": "string",
                  "ingredients": [
                    { "name": "정제수", "confidence": 0.95, "evidence": "정제수, 부틸렌글라이콜" }
                  ]
                }
                """;

        String userText = "Product type: cosmetic. Return JSON only.";
        Media media = toMedia(imageBytes, filename, contentType);

        long t0 = System.nanoTime();
        String rsp = chat.prompt()
                .system(system)
                .user(u -> u.text(userText).media(media))
                .call()
                .content();
        long t1 = System.nanoTime();
        log.info("[OCR.extract] latency={} ms", (t1 - t0) / 1_000_000);

        String json = stripToJson(rsp);
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            return new ExtractedIngredients(List.of(), List.of());
        }
        return postProcess(root);
    }

    /**
     * (옵션) 전체 OCR
     */
    public String ocrAllText(byte[] imageBytes, @Nullable String filename,
            @Nullable String contentType) {
        String system = """
                You are a strict OCR engine
                Return JSON ONLY -> {"text":"<full readable text>"}
                Rules:
                - Extract all visible text, preserve line breaks.
                - No summarize/guessing. No markdown.
                """;
        String user = "Perform OCR for the full image and return JSON only.";

        Media media = toMedia(imageBytes, filename, contentType);

        long t0 = System.nanoTime();
        String rsp = chat.prompt()
                .system(system)
                .user(u -> u.text(user).media(media))
                .call()
                .content();
        long t1 = System.nanoTime();
        log.info("[OCR.ocrAllText] latency={} ms", (t1 - t0) / 1_000_000);

        String json = stripToJson(rsp);
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("text").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    // --- 후처리 ---
    private ExtractedIngredients postProcess(JsonNode root) {
        boolean found = root.path("found").asBoolean(false);
        String section = root.path("section").asText("none");
        String rawBlock = root.path("raw_block").asText("");
        String rawLower = rawBlock.toLowerCase(Locale.ROOT);

        boolean hasHeader = NUTRITION_HEADER.stream()
                .anyMatch(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)));
        long macroHits = MACROS.stream().filter(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)))
                .count();

        if (!found || "none".equals(section) || (hasHeader && macroHits >= 2)) {
            return new ExtractedIngredients(List.of(), List.of());
        }

        LinkedHashSet<String> approved = new LinkedHashSet<>();
        String normBlock = norm(rawBlock);

        if (root.has("ingredients") && root.get("ingredients").isArray()) {
            for (JsonNode n : root.get("ingredients")) {
                String nm = n.path("name").asText("").trim();
                double conf = n.path("confidence").asDouble(0.0);
                String ev = n.path("evidence").asText("");

                if (nm.isEmpty()) {
                    continue;
                }
                if (conf < 0.75) {
                    continue;
                }

                boolean inBlock = normBlock.contains(norm(nm));
                boolean inEv = norm(ev).contains(norm(nm));
                if (!inBlock && !inEv) {
                    continue;
                }

                if (nm.length() < 2 || nm.length() > 50) {
                    continue;
                }
                if (nm.matches(".*[™®].*")) {
                    continue;
                }
                if (UNIT_PATTERN.matcher(nm).find()) {
                    continue;
                }
                if (STOPWORDS.contains(nm)) {
                    continue;
                }

                approved.add(nm);
            }
        }

        List<String> kr = new ArrayList<>();
        List<String> en = new ArrayList<>();
        for (String nm : approved) {
            if (nm.matches(".*[가-힣].*")) {
                kr.add(nm);
            } else {
                en.add(nm);
            }
        }
        return new ExtractedIngredients(kr, en);
    }

    // --- helpers ---
    private static Media toMedia(byte[] bytes, @Nullable String filename,
            @Nullable String contentType) {
        byte[] prepped = ImagePreprocessor.preprocess(bytes);
        var res = new ByteArrayResource(prepped) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "upload";
            }
        };
        var ct = MimeTypeUtils.parseMimeType(
                (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType);
        return Media.builder().mimeType(ct).data(prepped).build();
    }

    private static String norm(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[\\s\"'\\[\\]（）()]+", "")
                .replaceAll("[·,/|;:\\.\\-]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String stripToJson(String s) {
        if (s == null) {
            return "";
        }
        Matcher m = JSON_BLOCK.matcher(s.trim());
        if (m.find()) {
            return m.group();
        }
        return s.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
    }

    // DTO
    public record ExtractedIngredients(List<String> ingredientsKr, List<String> ingredientsEn) {

    }
}
