package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.ai.OpenAiNarrative;
import com.refit.app.domain.analysis.ai.OpenAiNarrative.SupplementTwoBlocks;
import com.refit.app.domain.analysis.ai.OpenAiVisionOcr;
import com.refit.app.domain.analysis.dto.AnalysisHairConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthInfoDto;
import com.refit.app.domain.analysis.dto.AnalysisSkinConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisStatus;
import com.refit.app.domain.analysis.dto.IngredientRule;
import com.refit.app.domain.analysis.dto.response.AnalysisResponseDto;
import com.refit.app.domain.analysis.dto.response.MemberStatusResponse;
import com.refit.app.domain.analysis.mapper.AnalysisMapper;
import com.refit.app.domain.analysis.policy.IngredientNormalizer;
import com.refit.app.domain.analysis.policy.ScoringPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final OpenAiVisionOcr ocr;
    private final OpenAiNarrative narrative;
    private final AnalysisMapper analysisMapper;

    private static final String[] SKIN_TYPE_NAME = {"건성", "중성", "지성", "복합성", "수부지"};

    @Override
    public MemberStatusResponse getMemberStatus(Long memberId) {
        AnalysisHealthInfoDto hi = analysisMapper.selectHealthInfo(memberId);
        AnalysisSkinConcernDto sc = analysisMapper.selectSkinConcern(memberId);
        AnalysisHealthConcernDto hc = analysisMapper.selectHealthConcern(memberId);
        AnalysisHairConcernDto hcHair = analysisMapper.selectHairConcern(memberId);

        int skinType = 1;
        if (sc != null && sc.getSkinType() != null) {
            skinType = sc.getSkinType();
        }
        String skinTypeName = SKIN_TYPE_NAME[Math.max(0, Math.min(4, skinType))];

        List<String> skinConcerns = new ArrayList<>();
        if (sc != null) {
            if (on(sc.getAtopic())) {
                skinConcerns.add("ATOPIC");
            }
            if (on(sc.getAcne())) {
                skinConcerns.add("ACNE");
            }
            if (on(sc.getWhitening())) {
                skinConcerns.add("WHITENING");
            }
            if (on(sc.getSebum())) {
                skinConcerns.add("SEBUM");
            }
            if (on(sc.getInnerDryness())) {
                skinConcerns.add("INNER_DRYNESS");
            }
            if (on(sc.getWrinkles())) {
                skinConcerns.add("WRINKLES");
            }
            if (on(sc.getEnlargedPores())) {
                skinConcerns.add("ENLARGED_PORES");
            }
            if (on(sc.getRedness())) {
                skinConcerns.add("REDNESS");
            }
            if (on(sc.getKeratin())) {
                skinConcerns.add("KERATIN");
            }
        }

        List<String> healthConcerns = new ArrayList<>();
        if (hc != null) {
            if (on(hc.getEyeHealth())) {
                healthConcerns.add("EYE_HEALTH");
            }
            if (on(hc.getFatigue())) {
                healthConcerns.add("FATIGUE");
            }
            if (on(hc.getSleepStress())) {
                healthConcerns.add("SLEEP_STRESS");
            }
            if (on(hc.getImmuneCare())) {
                healthConcerns.add("IMMUNE_CARE");
            }
            if (on(hc.getMuscleHealth())) {
                healthConcerns.add("MUSCLE_HEALTH");
            }
            if (on(hc.getGutHealth())) {
                healthConcerns.add("GUT_HEALTH");
            }
            if (on(hc.getBloodCirculation())) {
                healthConcerns.add("BLOOD_CIRCULATION");
            }
        }

        List<String> hairConcerns = new ArrayList<>();
        hcHair = analysisMapper.selectHairConcern(memberId);
        if (hcHair != null) {
            if (on(hcHair.getHairLoss())) {
                hairConcerns.add("HAIR_LOSS");
            }
            if (on(hcHair.getDamagedHair())) {
                hairConcerns.add("DAMAGED_HAIR");
            }
            if (on(hcHair.getScalpTrouble())) {
                hairConcerns.add("SCALP_TROUBLE");
            }
            if (on(hcHair.getDandruff())) {
                hairConcerns.add("DANDRUFF");
            }
        }

        var metrics = MemberStatusResponse.Metrics.builder()
                .bloodPressure(hi != null ? hi.getBloodPressure() : null)
                .bloodGlucose(hi != null ? hi.getBloodGlucose() : null)
                .totalCaloriesBurned(hi != null ? hi.getTotalCaloriesBurned() : null)
                .sleepSession(hi != null ? hi.getSleepSession() : null)
                .build();

        return MemberStatusResponse.builder()
                .memberId(memberId)
                .skinType(skinType)
                .skinTypeName(skinTypeName)
                .skinConcerns(skinConcerns)
                .healthConcerns(healthConcerns)
                .hairConcerns(hairConcerns)
                .metrics(metrics)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResponseDto analyzeImage(
            Long memberId,
            byte[] imageBytes,
            String productType,
            @Nullable String filename,
            @Nullable String contentType
    ) {
        long tStart = System.nanoTime();

        try {
            boolean isHealth = false;
            if (productType != null) {
                String p = productType.trim().toLowerCase();
                isHealth = p.contains("영양") || p.equals("health");
            }

            // ================== 영양제 ==================
            if (isHealth) {
                long t0 = System.nanoTime();
                String ocrText = ocr.ocrAllText(imageBytes, filename, contentType);
                long t1 = System.nanoTime();
                log.info("[Pipeline] OCR(health) latency={} ms", (t1 - t0) / 1_000_000);

                if (ocrText == null || ocrText.isBlank()) {
                    return AnalysisResponseDto.builder()
                            .status(AnalysisStatus.OCR_FAILURE)
                            .reason("Empty OCR")
                            .suggestion(suggestRetakeMessage())
                            .build();
                }
                if (!looksLikeLabelText(ocrText)) {
                    return AnalysisResponseDto.builder()
                            .status(AnalysisStatus.NOT_PRODUCT_LABEL)
                            .reason("Text not like supplement/cosmetic label")
                            .suggestion(suggestRetakeMessage())
                            .build();
                }

                CompletableFuture<SupplementTwoBlocks> futureSummary =
                        CompletableFuture.supplyAsync(
                                () -> narrative.buildSupplementTwoBlocksFromText(ocrText));

                SupplementTwoBlocks result = futureSummary.join();

                long t2 = System.nanoTime();
                log.info("[Pipeline] Narrative(health) latency={} ms", (t2 - t1) / 1_000_000);
                log.info("[Pipeline] Total latency={} ms", (t2 - tStart) / 1_000_000);

                return AnalysisResponseDto.builder()
                        .status(AnalysisStatus.OK)
                        .memberName("")
                        .matchRate(0)
                        .risky(List.of())
                        .caution(List.of())
                        .safe(List.of())
                        .riskyText(result.conditionCautions())
                        .cautionText("")
                        .safeText("")
                        .summary(result.benefits())
                        .supplementBenefits(result.benefits())
                        .supplementConditionCautions(result.conditionCautions())
                        .build();
            }

            // ================== 화장품 ==================
            // 1) FAST 경로: 전성분 블록만 뽑기 (8초 타임아웃)
            CompletableFuture<String> fastBlockFuture = CompletableFuture
                    .supplyAsync(
                            () -> ocr.extractIngredientBlockFast(imageBytes, filename, contentType))
                    .orTimeout(8, TimeUnit.SECONDS);

            String fastBlock = "";
            try {
                fastBlock = fastBlockFuture.join();
            } catch (Exception timeout) {
                log.warn("[Pipeline] FAST OCR timed out → fallback to full extract");
            }

            List<String> names;
            if (fastBlock != null && !fastBlock.isBlank()) {
                // 전성분 블록을 로컬 파싱(쉼표/슬래시/점 등)
                names = parseIngredientsBlockLocally(fastBlock);
                log.info("[Pipeline] FAST parse size={}", names.size());
            } else {
                // 2) 풀 추출(기존) (비동기)
                long t0 = System.nanoTime();
                var extracted = CompletableFuture
                        .supplyAsync(() -> ocr.extract(imageBytes, filename, contentType))
                        .join();
                long t1 = System.nanoTime();
                log.info("[Pipeline] OCR(cosmetic) latency={} ms", (t1 - t0) / 1_000_000);

                if ((extracted.ingredientsKr().isEmpty()) && (extracted.ingredientsEn()
                        .isEmpty())) {
                    return AnalysisResponseDto.builder()
                            .status(AnalysisStatus.NO_INGREDIENTS)
                            .reason("No ingredients section or too few tokens")
                            .suggestion(suggestRetakeMessage())
                            .build();
                }

                List<String> raw = new ArrayList<>();
                raw.addAll(extracted.ingredientsKr());
                raw.addAll(extracted.ingredientsEn());
                names = raw;
            }

            names = names.stream()
                    .map(IngredientNormalizer::normalize)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            // DB 룰 조회
            List<IngredientRule> rules =
                    names.isEmpty() ? List.of() : analysisMapper.selectByNames(names);
            Map<String, IngredientRule> ruleByName = rules.stream()
                    .collect(Collectors.toMap(
                            IngredientRule::getIngredientName,
                            r -> r,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            List<String> safe = new ArrayList<>();
            List<String> caution = new ArrayList<>();
            List<String> danger = new ArrayList<>();
            for (String n : names) {
                IngredientRule r = ruleByName.get(n);
                if (r == null) {
                    continue;
                }
                switch (r.getIngredientCategory()) {
                    case 0 -> safe.add(n);
                    case 1 -> caution.add(n);
                    case 2 -> danger.add(n);
                    default -> {
                    }
                }
            }

            var unknownAll = names.stream().filter(n -> !ruleByName.containsKey(n)).toList();
            List<String> unknown = unknownAll.size() > 8 ? unknownAll.subList(0, 8) : unknownAll;

            // 병렬로 스킨 concern 조회
            CompletableFuture<AnalysisSkinConcernDto> futureSkin =
                    CompletableFuture.supplyAsync(() -> analysisMapper.selectSkinConcern(memberId));
            var skin = futureSkin.join();

            var profile = new ScoringPolicy.Profile(
                    skin != null && (skin.getAcne() == 1 || skin.getAtopic() == 1
                            || skin.getRedness() == 1),
                    skin != null && skin.getAcne() == 1,
                    skin != null && skin.getAtopic() == 1,
                    skin != null && skin.getInnerDryness() == 1,
                    skin != null && skin.getRedness() == 1
            );

            int matchRate = ScoringPolicy.computeMatchRate(
                    names.size(),
                    safe.size() + caution.size() + danger.size(),
                    safe.size(), caution.size(), danger.size(),
                    profile
            );

            String memberName = analysisMapper.selectMemberNickname(memberId);
            if (StringUtils.isBlank(memberName)) {
                memberName = "사용자";
            }

            long tN0 = System.nanoTime();
            var classifyAndNar = narrative.classifyAndNarrate(danger, caution, safe, unknown,
                    memberName, matchRate);
            long tN1 = System.nanoTime();
            log.info("[Pipeline] Narrative(cosmetic) latency={} ms", (tN1 - tN0) / 1_000_000);
            log.info("[Pipeline] Total latency={} ms", (tN1 - tStart) / 1_000_000);

            danger = new ArrayList<>(classifyAndNar.finalRisky());
            caution = new ArrayList<>(classifyAndNar.finalCaution());
            safe = new ArrayList<>(classifyAndNar.finalSafe());

            var nar = classifyAndNar.narrative();

            return AnalysisResponseDto.builder()
                    .status(AnalysisStatus.OK)
                    .memberName(memberName)
                    .matchRate(matchRate)
                    .risky(danger)
                    .caution(caution)
                    .safe(safe)
                    .riskyText(nar.riskyText())
                    .cautionText(nar.cautionText())
                    .safeText(nar.safeText())
                    .summary(nar.summary())
                    .build();

        } catch (Exception e) {
            log.error("analyzeImage error", e);
            return AnalysisResponseDto.builder()
                    .status(AnalysisStatus.SERVER_ERROR)
                    .reason(e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? ""
                            : e.getMessage()))
                    .suggestion("일시적인 오류예요. 잠시 후 다시 시도해주세요.")
                    .build();
        }
    }

    private boolean on(@Nullable Integer v) {
        return v != null && v == 1;
    }

    private static boolean looksLikeLabelText(String text) {
        if (text == null) {
            return false;
        }
        String t = text.toLowerCase();
        int len = t.replaceAll("\\s+", "").length();
        int hits = 0;
        for (String k : new String[]{
                "전성분", "성분", "원재료", "영양", "영양성분", "ingredients", "nutrition", "mg", "%", "iu", "정",
                "캡슐", "cream", "serum", "lotion", "shampoo"
        }) {
            if (t.contains(k)) {
                hits++;
            }
        }
        return len >= 80 && hits >= 1;
    }

    private static String suggestRetakeMessage() {
        return "라벨이 또렷하게 보이도록 정면에서 촬영해주세요.\n"
                + "- 화장품: ‘전성분/성분’ 영역이 보이게\n"
                + "- 영양제: ‘영양성분표/주요 성분’이 보이게\n"
                + "- 흐림/반사/접힘/잘림 주의";
    }

    /**
     * FAST 블록 로컬 파서 (쉼표/세미콜론/슬래시/점 구분자)
     */
    private static List<String> parseIngredientsBlockLocally(String block) {
        if (block == null || block.isBlank()) {
            return List.of();
        }
        // 줄바꿈 → 공백, 공통 구분자 표준화
        String norm = block.replace('\n', ' ')
                .replace("ㆍ", ",")
                .replace("·", ",")
                .replace("│", ",")
                .replace("|", ",");
        String[] tokens = norm.split("[,;/·\\|•\\u00B7\\u2022]+");
        List<String> out = new ArrayList<>();
        for (String tk : tokens) {
            String s = tk.trim();
            if (s.isEmpty()) {
                continue;
            }
            // 괄호/단위 제거
            s = s.replaceAll("\\([^\\)]*\\)", "")
                    .replaceAll("\\[[^\\]]*\\]", "")
                    .replaceAll("\\{[^\\}]*\\}", "")
                    .replaceAll("\\s{2,}", " ")
                    .trim();
            if (s.length() < 2 || s.length() > 50) {
                continue;
            }
            out.add(s);
        }
        return out;
    }
}
