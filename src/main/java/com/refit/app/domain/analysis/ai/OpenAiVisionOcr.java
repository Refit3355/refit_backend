package com.refit.app.domain.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@Component
public class OpenAiVisionOcr {

    private final ChatClient chat;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiVisionOcr(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    // 마지막 JSON 객체만 잘라내는 세이프가드
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}\\s*$");

    // 성분/영양 관련 키워드(화장품 필터링 보조)
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
     * 화장품 전용: '전성분/성분' 섹션에서만 성분 추출
     */
    public ExtractedIngredients extract(byte[] imageBytes,
            @Nullable String filename,
            @Nullable String contentType) {

        String system = """
                You are a label analyst for cosmetics.
                TASK:
                - Extract ONLY exact ingredients from the image’s actual “Ingredients” section (e.g., 전성분/성분).
                - If you cannot visually locate such a section, return found=false and an empty list.
                - Do NOT guess/infer/translate/fabricate.
                
                OUTPUT: JSON ONLY.
                RULES:
                - First decide if an “Ingredients” section is visually present.
                - If present, return its raw text as `raw_block`.
                - Each item in `ingredients[]` MUST appear as a token in `raw_block` (ignoring separators/spaces).
                - For each item include `name`, `confidence` (0–1), `evidence` (a source line).
                - Exclude any item with confidence < 0.80 or that does not appear in raw_block/evidence.
                - Exclude amounts/units/percentages (mg, g, %, IU).
                - Exclude marketing phrases, warnings, brand names, trademarks (™, ®), standalone numbers.
                - Split on common delimiters (, / ; . | etc.), then de-duplicate.
                
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

        String rsp = chat.prompt()
                .system(system)
                .user(u -> u.text(userText).media(media))
                .stream()                 // Flux<String>
                .content()
                .collectList()            // Mono<List<String>>
                .map(list -> String.join("", list))
                .block();

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
     * (옵션) 화장품: 이미지 전체 텍스트 OCR
     */
    public String ocrAllText(byte[] imageBytes,
            @Nullable String filename,
            @Nullable String contentType) {
        String system = """
                You are a strict OCR engine.
                Return JSON ONLY -> {"text":"<full readable text>"}
                Rules:
                - Extract all visible text (Korean/English), including tables/headers/footnotes.
                - Preserve line breaks with \\n.
                - No summarize/guessing. No markdown.
                """;
        String user = "Perform OCR for the full image and return JSON only.";

        Media media = toMedia(imageBytes, filename, contentType);

        String rsp = chat.prompt()
                .system(system)
                .user(u -> u.text(user).media(media))
                .call()
                .content();

        String json = stripToJson(rsp);
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("text").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    // --- 후처리(화장품 전용) ---
    private ExtractedIngredients postProcess(JsonNode root) {
        boolean found = root.path("found").asBoolean(false);
        String section = root.path("section").asText("none");
        String rawBlock = root.path("raw_block").asText("");
        String rawLower = rawBlock.toLowerCase(Locale.ROOT);

        // 영양정보 테이블로 오판 방어
        boolean hasHeader = NUTRITION_HEADER.stream()
                .anyMatch(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)));
        long macroHits = MACROS.stream()
                .filter(k -> rawLower.contains(k.toLowerCase(Locale.ROOT))).count();

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
                if (conf < 0.80) {
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
        var res = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "upload";
            }
        };
        var ct = MimeTypeUtils.parseMimeType(
                (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType
        );
        return Media.builder().mimeType(ct).data(bytes).build();
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
