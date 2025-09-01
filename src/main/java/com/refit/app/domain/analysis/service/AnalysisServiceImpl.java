package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.ai.OpenAiNarrative;
import com.refit.app.domain.analysis.ai.OpenAiNarrative.SupplementTwoBlocks;
import com.refit.app.domain.analysis.ai.OpenAiVisionOcr;
import com.refit.app.domain.analysis.dto.AnalysisHairConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthInfoDto;
import com.refit.app.domain.analysis.dto.AnalysisSkinConcernDto;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        boolean isHealth = false;
        if (productType != null) {
            String p = productType.trim().toLowerCase();
            isHealth = p.contains("영양") || p.equals("health");
        }

        // ================== 영양제 ==================
        if (isHealth) {
            String ocrText = ocr.ocrAllText(imageBytes, filename, contentType);

            CompletableFuture<SupplementTwoBlocks> futureSummary =
                    CompletableFuture.supplyAsync(
                            () -> narrative.buildSupplementTwoBlocksFromText(ocrText));

            SupplementTwoBlocks result = futureSummary.join();

            return AnalysisResponseDto.builder()
                    .memberName("")
                    .matchRate(0)
                    .risky(List.of())
                    .caution(List.of())
                    .safe(List.of())
                    .riskyText(result.conditionCautions())
                    .cautionText("")
                    .safeText("")
                    .summary(result.benefits())
                    .build();
        }

        // ================== 화장품 ==================
        // 1) OCR 성분 추출 (비동기)
        CompletableFuture<OpenAiVisionOcr.ExtractedIngredients> futureExtract =
                CompletableFuture.supplyAsync(() -> ocr.extract(imageBytes, filename, contentType));

        // 2) DB Skin concern 조회 (병렬)
        CompletableFuture<AnalysisSkinConcernDto> futureSkin =
                CompletableFuture.supplyAsync(() -> analysisMapper.selectSkinConcern(memberId));

        // OCR 결과 join
        var extracted = futureExtract.join();

        List<String> raw = new ArrayList<>();
        raw.addAll(extracted.ingredientsKr());
        raw.addAll(extracted.ingredientsEn());

        List<String> names = raw.stream()
                .map(IngredientNormalizer::normalize)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

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
                default -> { /* no-op */ }
            }
        }

        // 미등록 성분 수집 (cap 적용으로 지연 방지)
        var unknownAll = names.stream().filter(n -> !ruleByName.containsKey(n)).toList();
        List<String> unknown = unknownAll.size() > 8 ? unknownAll.subList(0, 8) : unknownAll;

        // Skin concern join 및 스코어
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

        // ✅ 단일 호출: unknown 분류 + 내러티브 동시 수행 (지연 단축 핵심)
        var classifyAndNar = narrative.classifyAndNarrate(
                danger, caution, safe, unknown, memberName, matchRate
        );

        danger = new ArrayList<>(classifyAndNar.finalRisky());
        caution = new ArrayList<>(classifyAndNar.finalCaution());
        safe = new ArrayList<>(classifyAndNar.finalSafe());

        var nar = classifyAndNar.narrative();

        return AnalysisResponseDto.builder()
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
    }

    private boolean on(@Nullable Integer v) {
        return v != null && v == 1;
    }
}
