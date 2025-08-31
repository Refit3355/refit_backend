package com.refit.app.domain.analysis.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ConcernCatalog {

    public enum SkinType {건성, 중성, 지성, 복합성, 수부지}

    public static final Set<String> SKIN_CONCERNS = Set.of(
            "아토피", "여드름/민감성", "미백/잡티", "피지/블랙헤드", "속건조",
            "주름/탄력", "모공", "홍조", "각질"
    );
    public static final Set<String> HAIR_CONCERNS = Set.of(
            "탈모", "손상모", "두피트러블", "비듬/각질"
    );
    public static final Set<String> HEALTH_CONCERNS = Set.of(
            "눈건강", "만성피로", "수면/스트레스", "면역력", "근력", "장건강", "혈액순환"
    );

    // ★ 영문 코드 → 한글 라벨 매핑
    private static final Map<String, String> CODE_TO_LABEL = Map.ofEntries(
            // 피부
            Map.entry("ATOPIC", "아토피"),
            Map.entry("ACNE", "여드름/민감성"),
            Map.entry("WHITENING", "미백/잡티"),
            Map.entry("SEBUM", "피지/블랙헤드"),
            Map.entry("INNER_DRYNESS", "속건조"),
            Map.entry("WRINKLES", "주름/탄력"),
            Map.entry("ENLARGED_PORES", "모공"),
            Map.entry("REDNESS", "홍조"),
            Map.entry("KERATIN", "각질"),
            // 모발
            Map.entry("HAIR_LOSS", "탈모"),
            Map.entry("DAMAGED_HAIR", "손상모"),
            Map.entry("SCALP_TROUBLE", "두피트러블"),
            Map.entry("DANDRUFF", "비듬/각질"),
            // 건강
            Map.entry("EYE_HEALTH", "눈건강"),
            Map.entry("FATIGUE", "만성피로"),
            Map.entry("SLEEP_STRESS", "수면/스트레스"),
            Map.entry("IMMUNE_CARE", "면역력"),
            Map.entry("MUSCLE_HEALTH", "근력"),
            Map.entry("GUT_HEALTH", "장건강"),
            Map.entry("BLOOD_CIRCULATION", "혈액순환")
    );

    // 코드 리스트를 라벨 리스트로 변환
    private static List<String> toLabels(List<String> src) {
        if (src == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String s : src) {
            if (s == null) {
                continue;
            }
            out.add(CODE_TO_LABEL.getOrDefault(s, s)); // 미정의면 원문 유지
        }
        return out;
    }

    public static List<String> mergeAllConcerns(
            List<String> skin, List<String> health, List<String> hair, String skinTypeName
    ) {
        var out = new ArrayList<String>();
        out.addAll(filterNone(toLabels(skin)));
        out.addAll(filterNone(toLabels(health)));
        out.addAll(filterNone(toLabels(hair)));
        if (skinTypeName != null && !skinTypeName.isBlank()) {
            out.add("피부타입:" + skinTypeName);
        }
        return out;
    }

    private static List<String> filterNone(List<String> src) {
        return src == null ? List.of() : src.stream().filter(v -> !"해당없음".equals(v)).toList();
    }

    public static String toPromptString(List<String> concerns) {
        if (concerns == null || concerns.isEmpty()) {
            return "none";
        }
        return String.join(", ", concerns);
    }

    public static boolean isKnownConcern(String c) {
        return Stream.of(SKIN_CONCERNS, HAIR_CONCERNS, HEALTH_CONCERNS)
                .anyMatch(set -> set.contains(c));
    }
}
