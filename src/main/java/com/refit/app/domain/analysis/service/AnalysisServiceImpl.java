package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.ai.OpenAiNarrative;
import com.refit.app.domain.analysis.ai.OpenAiNarrative.ClassificationResult;
import com.refit.app.domain.analysis.ai.OpenAiNarrative.CosmeticNarrative;
import com.refit.app.domain.analysis.ai.OpenAiNarrative.SupplementTwoBlocks;
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
import com.refit.app.infra.ocr.OcrProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final OcrProvider ocrProvider;
    private final OpenAiNarrative narrative;
    private final AnalysisMapper analysisMapper;

    private static final String[] SKIN_TYPE_NAME = {"건성", "중성", "지성", "복합성", "수부지"};

    @Value("${analysis.debug-ocr:false}")
    private boolean debugOcr;

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
                String ocrText;
                try {
                    ocrText = ocrProvider.fullText(imageBytes, filename, contentType);
                } catch (Exception ex) {
                    log.warn("[OCR][health] error: {}", ex.toString());
                    return ocrFail("OCR_TIMEOUT_OR_ERROR");
                }
                long t1 = System.nanoTime();
                log.info("[Pipeline] OCR(health) latency={} ms", (t1 - t0) / 1_000_000);
                if (debugOcr) {
                    log.info("[OCR][health][raw {} chars]\n{}",
                            (ocrText == null ? 0 : ocrText.length()),
                            abbreviateForLog(ocrText, 6000));
                }

                if (ocrText == null || ocrText.isBlank()) {
                    return ocrFail("Empty OCR");
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

                // 영양제는 텍스트 2블록만 반환
                return AnalysisResponseDto.builder()
                        .status(AnalysisStatus.OK)
                        .memberName("")
                        .matchRate(0)
                        .risky(java.util.Collections.emptyList())
                        .caution(java.util.Collections.emptyList())
                        .safe(java.util.Collections.emptyList())
                        .riskyText(result.conditionCautions())
                        .cautionText("")
                        .safeText("")
                        .summary(result.benefits())
                        .supplementBenefits(java.util.List.of(result.benefits()))
                        .supplementConditionCautions(java.util.List.of(result.conditionCautions()))
                        .build();
            }

            // ================== 화장품 ==================
            long t0 = System.nanoTime();
            String ocrText;
            try {
                ocrText = ocrProvider.fullText(imageBytes, filename, contentType);
            } catch (Exception ex) {
                log.warn("[OCR][cosmetic] error: {}", ex.toString());
                return ocrFail("OCR_TIMEOUT_OR_ERROR");
            }
            long t1 = System.nanoTime();
            log.info("[Pipeline] OCR(cosmetic) latency={} ms", (t1 - t0) / 1_000_000);
            if (debugOcr) {
                log.info("[OCR][cosmetic][raw {} chars]\n{}",
                        (ocrText == null ? 0 : ocrText.length()),
                        abbreviateForLog(ocrText, 6000));
            }

            if (ocrText == null || ocrText.isBlank()) {
                return ocrFail("Empty OCR");
            }

            // 1) 한글 소프트 브레이크 결합 (정제\n수 → 정제수)
            String compact = collapseHangulSoftBreaks(ocrText);

            // 2) 전성분 블록 시도 → 실패 시 전체 텍스트로 파싱
            String block = extractIngredientsBlockFromOcr(compact);
            if (block.isBlank()) {
                block = compact;
            }

            // 3) 로컬 파서로 성분 후보 추출
            List<String> names = parseIngredientsBlockLocally(block);
            if (names.isEmpty()) {
                return AnalysisResponseDto.builder()
                        .status(AnalysisStatus.NO_INGREDIENTS)
                        .reason("No ingredients section or too few tokens")
                        .suggestion(suggestRetakeMessage())
                        .build();
            }

            // 4) 정규화 + dedupe
            names = names.stream()
                    .map(IngredientNormalizer::normalize)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            // 5) DB 룰 매칭
            List<IngredientRule> rules = analysisMapper.selectByNames(names);
            Map<String, IngredientRule> ruleByName = rules.stream()
                    .collect(Collectors.toMap(
                            IngredientRule::getIngredientName,
                            r -> r,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            LinkedHashSet<String> finalSafe = new LinkedHashSet<>();
            LinkedHashSet<String> finalCaution = new LinkedHashSet<>();
            LinkedHashSet<String> finalRisky = new LinkedHashSet<>();

            for (String n : names) {
                IngredientRule r = ruleByName.get(n);
                if (r != null) {
                    switch (r.getIngredientCategory()) {
                        case 0 -> finalSafe.add(n);
                        case 1 -> finalCaution.add(n);
                        case 2 -> finalRisky.add(n);
                        default -> {
                        }
                    }
                }
            }

            // 6) DB에 없는 unknown만 LLM으로 초간단 분류
            List<String> unknown = names.stream().filter(n -> !ruleByName.containsKey(n)).toList();
            if (!unknown.isEmpty()) {
                ClassificationResult cr = narrative.classifyListFast(unknown);
                cr.safe().forEach(finalSafe::add);
                cr.caution().forEach(finalCaution::add);
                cr.risky().forEach(finalRisky::add);
            }

            // 7) Match rate
            var profile = loadSkinProfile(memberId);
            int matchRate = ScoringPolicy.computeMatchRate(
                    names.size(),
                    finalSafe.size() + finalCaution.size() + finalRisky.size(),
                    finalSafe.size(), finalCaution.size(), finalRisky.size(),
                    profile
            );

            String memberName = analysisMapper.selectMemberNickname(memberId);
            if (StringUtils.isBlank(memberName)) {
                memberName = "사용자";
            }

            // 8) 짧은 내러티브
            CosmeticNarrative nar = narrative.buildCosmeticNarrative(
                    new ArrayList<>(finalRisky),
                    new ArrayList<>(finalCaution),
                    new ArrayList<>(finalSafe),
                    memberName,
                    matchRate
            );

            // ★ 내러티브 검증: 비었으면 실패 처리
            if (shouldFailDueToMissingNarrative(nar, finalRisky, finalCaution, finalSafe)) {
                log.warn(
                        "[Pipeline] Narrative missing -> fail. risky/caution/safe sizes = {}/{}/{}",
                        finalRisky.size(), finalCaution.size(), finalSafe.size());
                return AnalysisResponseDto.builder()
                        .status(AnalysisStatus.SERVER_ERROR)
                        .reason("NARRATIVE_EMPTY")
                        .suggestion("일시적인 오류예요. 잠시 후 다시 시도해주세요.")
                        .build();
            }

            // 9) 결과
            long tN1 = System.nanoTime();
            log.info("[Pipeline] Total latency={} ms", (tN1 - tStart) / 1_000_000);

            return AnalysisResponseDto.builder()
                    .status(AnalysisStatus.OK)
                    .memberName(memberName)
                    .matchRate(matchRate)
                    .risky(new ArrayList<>(finalRisky))
                    .caution(new ArrayList<>(finalCaution))
                    .safe(new ArrayList<>(finalSafe))
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

    // ---- helpers ----

    private ScoringPolicy.Profile loadSkinProfile(Long memberId) {
        CompletableFuture<AnalysisSkinConcernDto> futureSkin =
                CompletableFuture.supplyAsync(() -> analysisMapper.selectSkinConcern(memberId));
        var skin = futureSkin.join();

        return new ScoringPolicy.Profile(
                skin != null && (skin.getAcne() == 1 || skin.getAtopic() == 1
                        || skin.getRedness() == 1),
                skin != null && skin.getAcne() == 1,
                skin != null && skin.getAtopic() == 1,
                skin != null && skin.getInnerDryness() == 1,
                skin != null && skin.getRedness() == 1
        );
    }

    private AnalysisResponseDto ocrFail(String reason) {
        return AnalysisResponseDto.builder()
                .status(AnalysisStatus.OCR_FAILURE)
                .reason(reason)
                .suggestion(suggestRetakeMessage())
                .build();
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
     * 한글 소프트 브레이크 제거: 줄바꿈 사이가 한글-한글이면 붙이기
     */
    private static String collapseHangulSoftBreaks(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("(?<=[가-힣])\\s*\\n\\s*(?=[가-힣])", "");
    }

    /**
     * 전성분 블록 슬라이스(없으면 "")
     */
    private static String extractIngredientsBlockFromOcr(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("\\r", "")
                .replaceAll("\\t", " ")
                .replaceAll(" +", " ");

        String startRegex =
                "(?i)(전\\s*성\\s*분|성\\s*분|INGREDIENTS?)\\s*[:：]?\\s*|(?m)^(전성분|성분|Ingredients?)\\b";
        String endRegex =
                "(?i)(주의사항|사용법|보관방법|원재료|영양성분|전성분표|제조사|고객센터|사용상\\s*주의|효능|효과)";

        java.util.regex.Matcher mStart =
                java.util.regex.Pattern.compile(startRegex).matcher(t);
        if (!mStart.find()) {
            return "";
        }

        int from = mStart.end();
        int to = t.length();

        java.util.regex.Matcher mEnd =
                java.util.regex.Pattern.compile(endRegex).matcher(t.substring(from));
        if (mEnd.find()) {
            to = from + mEnd.start();
        }

        return t.substring(from, to).trim()
                .replaceAll("(?<!\\n),\\s*", ", ")
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }

    /**
     * FAST 파서: 구분자 기준 + 괄호/잡음 제거
     */
    private static List<String> parseIngredientsBlockLocally(String block) {
        if (block == null || block.isBlank()) {
            return List.of();
        }
        String norm = block
                .replace("ㆍ", ",").replace("·", ",").replace("│", ",").replace("|", ",")
                .replaceAll("\\u00B7|\\u2022", ",");
        norm = norm.replace('\r', ' ').replace('\n', ' ');

        String[] tokens = norm.split("[,;/\\\\]+");
        List<String> out = new ArrayList<>();
        for (String tk : tokens) {
            String s = tk.trim();
            if (s.isEmpty()) {
                continue;
            }

            s = s.replaceAll("\\([^\\)]*\\)", "")
                    .replaceAll("\\[[^\\]]*\\]", "")
                    .replaceAll("\\{[^\\}]*\\}", "");

            s = s.replaceAll("\\s{2,}", " ").trim();

            if (s.length() < 2 || s.length() > 50) {
                continue;
            }

            out.add(s);
        }
        LinkedHashSet<String> set = new LinkedHashSet<>(out);
        return new ArrayList<>(set);
    }

    /**
     * 로그 너무 길면 잘라서
     */
    private static String abbreviateForLog(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + " …(truncated)";
    }

    // ===== 내러티브 미존재 실패 처리 헬퍼 =====

    private static boolean isBlankOrPlaceholder(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return true;
        }
        return "요약 생성에 실패했어요.".equals(t) || "해당되는 성분은 없습니다.".equals(t);
    }

    private static boolean isNarrativeMissing(OpenAiNarrative.CosmeticNarrative nar) {
        if (nar == null) {
            return true;
        }
        boolean summaryBad = isBlankOrPlaceholder(nar.summary());
        boolean groupsAllBad =
                isBlankOrPlaceholder(nar.riskyText()) &&
                        isBlankOrPlaceholder(nar.cautionText()) &&
                        isBlankOrPlaceholder(nar.safeText());
        // 요약이 없거나, 세 그룹 설명이 모두 무의미하면 실패로 간주
        return summaryBad || groupsAllBad;
    }

    private static boolean shouldFailDueToMissingNarrative(
            OpenAiNarrative.CosmeticNarrative nar,
            java.util.Collection<String> risky,
            java.util.Collection<String> caution,
            java.util.Collection<String> safe
    ) {
        boolean listsHaveContent =
                (risky != null && !risky.isEmpty()) ||
                        (caution != null && !caution.isEmpty()) ||
                        (safe != null && !safe.isEmpty());
        return isNarrativeMissing(nar) && listsHaveContent;
    }
}
