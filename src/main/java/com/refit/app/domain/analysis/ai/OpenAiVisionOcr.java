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

    // 1번과 동일한 규칙들
    private static final Set<String> NUTRITION_HEADER = Set.of(
            "영양정보", "영양성분", "영양성분표", "1일 영양성분 기준치", "1일 영양성분 기준치에 대한 비율", "열량", "kcal",
            "nutrition facts");
    private static final Set<String> MACROS = Set.of(
            "나트륨", "탄수화물", "당류", "지방", "단백질", "콜레스테롤", "포화지방", "트랜스지방",
            "sodium", "carbohydrate", "sugars", "fat", "protein", "cholesterol", "saturated fat",
            "trans fat");
    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+)?\\s*(?:mg|㎎|g|µg|mcg|iu|%)\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> STOPWORDS = Set.of(
            "전성분", "성분", "원재료", "원재료명", "성분표", "주의사항", "사용법", "보관방법", "섭취방법", "주의", "경고", "알레르기",
            "부작용", "브랜드", "제조사");
    private static final Pattern EPA_PAT = Pattern.compile("\\bEPA\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DHA_PAT = Pattern.compile("\\bDHA\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern O3_PAT = Pattern.compile("오메가[-\\s]?3|omega[-\\s]?3",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VITD_PAT = Pattern.compile(
            "비타민\\s*D\\s*\\d*|vitamin\\s*d\\b\\s*\\d*", Pattern.CASE_INSENSITIVE);
    private static final Pattern VITE_PAT = Pattern.compile(
            "비타민\\s*E\\s*\\d*|vitamin\\s*e\\b\\s*\\d*", Pattern.CASE_INSENSITIVE);

    /**
     * 화장품/영양제 공통: 성분 섹션에서만 추출
     */
    public ExtractedIngredients extract(byte[] imageBytes,
            String productType,  // "화장품" | "영양제" | "beauty" | "health"
            @Nullable String filename,
            @Nullable String contentType) {

        boolean isHealth = false;
        if (productType != null) {
            String p = productType.trim().toLowerCase(Locale.ROOT);
            isHealth = p.contains("영양") || "health".equals(p);
        }

        String system = """
                You are a label analyst for cosmetics and dietary supplements.
                
                TASK:
                Extract ONLY the exact ingredients from the image’s actual “Ingredients” section (e.g., 원재료명, 전성분, 성분).
                If you cannot visually locate such a section in the image, return found=false and an empty list.
                Do NOT guess, infer, translate, or fabricate.
                
                OUTPUT:
                You must answer in a single JSON object only.
                No explanations, no markdown, no text outside JSON.
                
                RULES:
                - First decide if an “Ingredients” section is visually present in the image.
                - If present, return its raw text as `raw_block`.
                - Each item in `ingredients[]` MUST appear as a token in the `raw_block` (ignoring separators and spaces).
                - Along with `name`, include `confidence` (0–1) and `evidence` (a source line from the raw section).
                - Exclude any item with confidence < 0.80 or that does not appear in `raw_block/evidence`.
                - Exclude amounts/units/percentages (e.g., mg, g, %, IU).
                - Exclude marketing phrases, warnings, brand names, trademarks (™ , ®), and standalone numbers that are not part of the ingredient name.
                - Split on common delimiters (comma, slash, semicolon, dot, vertical bar, etc.), then de-duplicate.
                - If only a Nutrition Facts table is present (calories/열량, sodium/나트륨, carbohydrates/탄수화물, fat/지방, protein/단백질, etc.) without an ingredients section, return found=false.
                
                SCHEMA:
                {
                  "found": true|false,
                  "section": "원재료명|전성분|성분|none",
                  "raw_block": "the exact raw text of the section",
                  "ingredients": [
                    { "name": "정제수", "confidence": 0.95, "evidence": "정제수, 부틸렌글라이콜" }
                  ]
                }
                """;
        if (isHealth) {
            system = system.replace(
                    "RULES:",
                    "RULES:\n- IMPORTANT (health): If both a Nutrition Facts table and functional active ingredients "
                            + "(e.g., Omega-3, EPA, DHA, Vitamin D, Vitamin E) appear, you MUST still extract those functional "
                            + "active ingredients even if they appear inside the nutrition box."
            );
        }

        String userText = """
                Product type hint: %s
                Return JSON only.
                """.formatted(isHealth ? "health" : "cosmetic");

        Media media = toMedia(imageBytes, filename, contentType);

        String rsp = chat.prompt().system(system).user(u -> u.text(userText).media(media)).call()
                .content();

        String json = stripToJson(rsp);

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            return new ExtractedIngredients(List.of(), List.of());
        }

        return postProcess(root, isHealth);
    }

    /**
     * 영양제용: 이미지 전체 텍스트 OCR (개인화 X 요약용)
     */
    public String ocrAllText(byte[] imageBytes,
            @Nullable String filename,
            @Nullable String contentType) {
        String system = """
                You are a strict OCR engine.
                Return JSON ONLY with exactly: { "text": "<the full readable text from the image>" }.
                - Extract all visible text (Korean/English), including tables, headers, footnotes.
                - Preserve line breaks with \\n.
                - Do NOT summarize or interpret. Do NOT add or guess words that are not present.
                - No markdown, no extra keys. JSON only.
                """;
        String user = "Perform OCR for the full image and return JSON only.";

        Media media = toMedia(imageBytes, filename, contentType);
        String rsp = chat.prompt().system(system).user(u -> u.text(user).media(media)).call()
                .content();

        String json = stripToJson(rsp);
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("text").asText(""); // 없으면 빈 문자열
        } catch (Exception e) {
            return "";
        }
    }

    // --- 후처리 ---
    private ExtractedIngredients postProcess(JsonNode root, boolean isHealth) {
        boolean found = root.path("found").asBoolean(false);
        String section = root.path("section").asText("none");
        String rawBlock = root.path("raw_block").asText("");
        String rawLower = rawBlock.toLowerCase(Locale.ROOT);

        boolean hasHeader = NUTRITION_HEADER.stream()
                .anyMatch(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)));
        long macroHits = MACROS.stream().filter(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)))
                .count();

        if ((!isHealth) && (!found || "none".equals(section) || (hasHeader && macroHits >= 2))) {
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

        if (isHealth && !rawBlock.isBlank()) {
            if (EPA_PAT.matcher(rawBlock).find()) {
                approved.add("EPA");
            }
            if (DHA_PAT.matcher(rawBlock).find()) {
                approved.add("DHA");
            }
            if (O3_PAT.matcher(rawBlock).find()) {
                approved.add("오메가3");
            }
            if (VITD_PAT.matcher(rawBlock).find()) {
                approved.add("비타민D");
            }
            if (VITE_PAT.matcher(rawBlock).find()) {
                approved.add("비타민E");
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

    public record ExtractedIngredients(List<String> ingredientsKr, List<String> ingredientsEn) {

    }
}
