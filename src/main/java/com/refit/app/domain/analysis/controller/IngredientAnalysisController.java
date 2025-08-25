package com.refit.app.domain.analysis.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/analysis")
public class IngredientAnalysisController {

    private final ChatClient chat;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngredientAnalysisController(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @PostMapping(
            value = "/product",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> extractIngredients(
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "mode", required = false) String mode // beauty | health | null
    ) throws Exception {

        byte[] imageBytes = file.getBytes();
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "image/jpeg";

        String system = """
                너는 화장품/영양제 라벨 분석가다.
                반드시 이미지 속 '원재료/전성분' 섹션(예: 원재료명, 전성분, 성분)을 실제 사진에서 찾은 경우에만
                그 섹션의 원문 그대로 성분명을 추출한다. 추측/보정/번역/임의 생성 금지.
                
                출력은 JSON 하나, 설명/코드블록 금지.
                규칙:
                - 먼저 이미지에서 섹션 존재 여부를 판단한다.
                - 섹션이 보이면 그 섹션의 원문 블록(raw_block)을 함께 반환한다.
                - ingredients[] 각 항목은 raw_block 안에 실제 토큰으로 존재해야 하며(공백/구분기호 제거 제외),
                  confidence(0~1)와 evidence(해당 항목이 포함된 원문 라인)를 함께 반환한다.
                - confidence < 0.80 이거나 raw_block/evidence에 존재하지 않으면 제외한다.
                - 괄호/함량/광고문구/주의문구/브랜드/마크/상표(™ ®)/단위/숫자는 제외.
                - 구분자(, / ; · . | 등)로 분리하고, 동일 성분은 중복 제거.
                - 섹션이 보이지 않으면 found=false, ingredients=[] 로 반환한다.
                - '영양정보/영양성분표'만 있고 원재료/전성분 섹션이 없으면 found=false 로 간주한다.
                
                출력 스키마:
                {
                  "found": true|false,
                  "section": "원재료명|전성분|성분|none",
                  "raw_block": "섹션 원문 그대로",
                  "ingredients": [
                    { "name": "정제수", "confidence": 0.95, "evidence": "정제수, 부틸렌글라이콜" }
                  ]
                }
                """;

        String user = """
                제품 유형(힌트): %s
                JSON만 반환.
                """.formatted(mode == null ? "unknown" : mode);

        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(contentType))
                .data(imageBytes)
                .build();

        String rsp = chat.prompt()
                .system(system)
                .user(u -> u.text(user).media(media))
                .call()
                .content();

        return postProcess(rsp, mode);
    }

    // ---------- 후처리: “없는 성분” 구조적으로 컷 ----------

    // 영양성분표(칼로리/나트륨 등)만 있는 경우 조기 종료 판단용 키워드
    private static final Set<String> NUTRITION_KEYWORDS = Set.of(
            "영양정보", "영양성분", "1일 영양성분 기준치", "1일 영양성분 기준치에 대한 비율",
            "나트륨", "탄수화물", "당류", "지방", "단백질", "콜레스테롤",
            "포화지방", "트랜스지방", "kcal", "mg", "g", "%"
    );

    private record Ingr(String name, double confidence, String evidence) {

    }

    private Map<String, Object> postProcess(String rsp, String mode) throws Exception {
        JsonNode root = objectMapper.readTree(rsp);
        boolean found = root.path("found").asBoolean(false);
        String section = root.path("section").asText("none");
        String rawBlock = root.path("raw_block").asText("");

        // 1) 영양성분표로 보이면 바로 빈 배열 반환
        String rawLower = rawBlock.toLowerCase(Locale.ROOT);
        long nutritionHits = NUTRITION_KEYWORDS.stream()
                .filter(k -> rawLower.contains(k.toLowerCase(Locale.ROOT))).count();
        if (!found || "none".equals(section) || nutritionHits >= 3) {
            return Map.of(
                    "mode", mode == null ? "unknown" : mode,
                    "count", 0,
                    "ingredients", List.of()
            );
        }

        // 2) 후보 성분 파싱
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

        // 3) 원문 포함 검증 + 임계치 + 휴리스틱 컷 + 중복 제거
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
                continue;                 // 원문 미포함 → 컷
            }

            if (nm.length() < 2 || nm.length() > 50) {
                continue;
            }
            if (nm.matches(".*[™®%0-9㎎mgMG].*")) {
                continue;   // 상표/함량/숫자 컷
            }
            if (STOPWORDS.contains(nm)) {
                continue;            // 전형적 문구 컷
            }

            approved.add(nm); // 중복 제거
        }

        List<String> ingredients = new ArrayList<>(approved);
        return Map.of(
                "mode", mode == null ? "unknown" : mode,
                "count", ingredients.size(),
                "ingredients", ingredients
        );
    }

    // 불용어(전성분 표제/문구 등)
    private static final Set<String> STOPWORDS = Set.of(
            "전성분", "성분", "원재료", "원재료명", "성분표", "주의사항", "사용법", "브랜드", "제조사"
    );

    // 비교용 정규화: 공백/괄호/구분기호 제거 + 소문자
    private static String norm(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[\\s\"'\\[\\]（）()]+", "")
                .replaceAll("[·,/|;:.-]+", "")
                .toLowerCase(Locale.ROOT);
    }
}
