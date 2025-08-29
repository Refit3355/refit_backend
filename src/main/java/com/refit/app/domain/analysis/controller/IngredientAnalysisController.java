package com.refit.app.domain.analysis.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.analysis.dto.response.IngredientAnalysisResponse;
import com.refit.app.domain.analysis.service.IngredientReportService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/analysis")
public class IngredientAnalysisController {

    private final ChatClient chat;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IngredientReportService reportService;

    public IngredientAnalysisController(ChatClient.Builder builder,
            IngredientReportService reportService) {
        this.chat = builder.build();
        this.reportService = reportService;
    }

    @PostMapping(
            value = "/product",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> extractIngredients(
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "mode", required = false) String mode // "beauty" | "health" | null
    ) throws Exception {

        byte[] imageBytes = file.getBytes();
        String contentType =
                StringUtils.hasText(file.getContentType()) ? file.getContentType() : "image/jpeg";
        boolean isHealth = "health".equalsIgnoreCase(mode);

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

        String user = """
                Product type hint: %s
                Return JSON only.
                """.formatted(isHealth ? "health" : (mode == null ? "unknown" : mode));

        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(contentType))
                .data(imageBytes)
                .build();

        String rsp = chat.prompt().system(system).user(u -> u.text(user).media(media)).call()
                .content();

        JsonNode root;
        try {
            root = objectMapper.readTree(rsp); // JSON only 보장 못하면 빈 결과
        } catch (Exception e) {
            return Map.of("mode", mode == null ? "unknown" : mode, "count", 0, "ingredients",
                    List.of());
        }

        return postProcess(root, mode);
    }

    // ---------- 후처리 ----------
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
            "\\b\\d+(?:\\.\\d+)?\\s*(?:mg|㎎|g|µg|mcg|iu|%)\\b", Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> STOPWORDS = Set.of(
            "전성분", "성분", "원재료", "원재료명", "성분표", "주의사항", "사용법", "보관방법", "섭취방법", "주의", "경고", "알레르기",
            "부작용", "브랜드", "제조사"
    );

    private static final Pattern EPA_PAT = Pattern.compile("\\bEPA\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DHA_PAT = Pattern.compile("\\bDHA\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern O3_PAT = Pattern.compile("오메가[-\\s]?3|omega[-\\s]?3",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VITD_PAT = Pattern.compile(
            "비타민\\s*D\\s*\\d*|vitamin\\s*d\\b\\s*\\d*", Pattern.CASE_INSENSITIVE);
    private static final Pattern VITE_PAT = Pattern.compile(
            "비타민\\s*E\\s*\\d*|vitamin\\s*e\\b\\s*\\d*", Pattern.CASE_INSENSITIVE);

    private Map<String, Object> postProcess(JsonNode root, String mode) {
        boolean isHealth = "health".equalsIgnoreCase(mode);

        boolean found = root.path("found").asBoolean(false);
        String section = root.path("section").asText("none");
        String rawBlock = root.path("raw_block").asText("");
        String rawLower = rawBlock.toLowerCase(Locale.ROOT);

        boolean hasHeader = NUTRITION_HEADER.stream()
                .anyMatch(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)));
        long macroHits = MACROS.stream().filter(k -> rawLower.contains(k.toLowerCase(Locale.ROOT)))
                .count();

        if ((!isHealth) && (!found || "none".equals(section) || (hasHeader && macroHits >= 2))) {
            return Map.of("mode", mode == null ? "unknown" : mode, "count", 0, "ingredients",
                    List.of());
        }

        record Ingr(String name, double confidence, String evidence) {

        }
        List<Ingr> items = new ArrayList<>();
        if (root.has("ingredients") && root.get("ingredients").isArray()) {
            for (JsonNode n : root.get("ingredients")) {
                items.add(new Ingr(
                        n.path("name").asText(""),
                        n.path("confidence").asDouble(0.0),
                        n.path("evidence").asText("")
                ));
            }
        }

        String normBlock = norm(rawBlock);
        LinkedHashSet<String> approved = new LinkedHashSet<>();

        for (Ingr g : items) {
            String nm = g.name() == null ? "" : g.name().trim();
            if (nm.isEmpty()) {
                continue;
            }
            if (g.confidence() < 0.80) {
                continue;
            }

            boolean inBlock = normBlock.contains(norm(nm));
            boolean inEv = norm(g.evidence()).contains(norm(nm));
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

        // health fallback: raw_block에서 반드시 보강 추출
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

        List<String> ingredients = new ArrayList<>(approved);
        return Map.of(
                "mode", mode == null ? "unknown" : mode,
                "count", ingredients.size(),
                "ingredients", ingredients
        );
    }

    private static String norm(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[\\s\"'\\[\\]（）()]+", "")
                .replaceAll("[·,/|;:.-]+", "")
                .toLowerCase(Locale.ROOT);
    }

    @PostMapping("/report")
    public IngredientAnalysisResponse analyzeReport(
            Authentication authentication,
            @RequestBody List<String> ingredients) {

        Long memberId = (Long) authentication.getPrincipal();
        return reportService.analyze(memberId, ingredients);
    }
}
