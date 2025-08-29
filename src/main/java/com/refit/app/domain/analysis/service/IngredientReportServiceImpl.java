package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.dto.response.IngredientAnalysisResponse;
import com.refit.app.domain.analysis.dto.response.MemberStatusResponse;
import com.refit.app.domain.analysis.policy.RiskPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class IngredientReportServiceImpl implements IngredientReportService {

    private final ChatClient chat;
    // 기존: member 상태 조회 서비스
    private final AnalysisService analysisService;
    private final RiskPolicy riskPolicy;

    public IngredientReportServiceImpl(ChatClient.Builder builder, AnalysisService analysisService,
            RiskPolicy riskPolicy) {
        this.chat = builder.build();
        this.analysisService = analysisService;
        this.riskPolicy = riskPolicy;
    }

    @Override
    public IngredientAnalysisResponse analyze(Long memberId, List<String> ingredients) {
        MemberStatusResponse st = analysisService.getMemberStatus(memberId);

        List<String> safe = new ArrayList<>();
        List<String> caution = new ArrayList<>();
        List<String> risky = new ArrayList<>();
        int matched = 0;

        Map<String, Object> metrics = new HashMap<>();
        if (st.getMetrics() != null) {
            metrics.put("bloodPressure", st.getMetrics().getBloodPressure());
            metrics.put("bloodGlucose", st.getMetrics().getBloodGlucose());
            metrics.put("totalCaloriesBurned", st.getMetrics().getTotalCaloriesBurned());
            metrics.put("sleepSession", st.getMetrics().getSleepSession());
        }

        boolean sensitiveProfile = hasSensitiveProfile(st);

        for (String ingr : ingredients) {
            RiskPolicy.RiskResult r = riskPolicy.evaluate(
                    ingr, st.getSkinConcerns(), st.getHealthConcerns(), st.getHairConcerns(),
                    metrics);

            switch (r.getRisk()) {
                case "high" -> {
                    risky.add(ingr);
                    matched++;
                }
                case "medium" -> {
                    caution.add(ingr);
                    matched++;
                }
                case "low" -> {
                    if (sensitiveProfile) {
                        caution.add(ingr);
                        matched++;
                    } else {
                        safe.add(ingr);
                    }
                }
                default -> safe.add(ingr);
            }
        }

        double matchRate =
                ingredients.isEmpty() ? 0.0 : (double) matched / ingredients.size() * 100.0;

        String summary = generatePersonalSummary(
                st,
                ingredients,
                risky,
                caution,
                safe,
                matchRate
        );

        return IngredientAnalysisResponse.builder()
                .memberId(memberId)
                .totalIngredients(ingredients.size())
                .matchRate(matchRate)
                .safeIngredients(safe)
                .cautionIngredients(caution)
                .riskyIngredients(risky)
                .summary(summary)
                .build();
    }

    private boolean hasSensitiveProfile(MemberStatusResponse st) {
        var skin = st.getSkinConcerns();
        String type = st.getSkinTypeName();
        boolean hasSensitive = (skin != null) && (skin.contains("여드름/민감성") || skin.contains("홍조"));
        boolean oily = type != null && (type.contains("지성") || type.contains("수부지"));
        return hasSensitive || oily;
    }

    private String generatePersonalSummary(
            MemberStatusResponse st,
            List<String> allIngredients,
            List<String> risky,
            List<String> caution,
            List<String> safe,
            double matchRate
    ) {
        // 사용자 프로필을 그대로 전달
        String concernsStr = String.join(", ",
                st.getSkinConcerns() != null ? st.getSkinConcerns() : List.of());
        String healthStr = String.join(", ",
                st.getHealthConcerns() != null ? st.getHealthConcerns() : List.of());
        String hairStr = String.join(", ",
                st.getHairConcerns() != null ? st.getHairConcerns() : List.of());

        String metricsStr = (st.getMetrics() == null) ? "none" : String.format(
                "bloodPressure=%s, bloodGlucose=%s, totalCaloriesBurned=%s, sleepSession=%s",
                String.valueOf(st.getMetrics().getBloodPressure()),
                String.valueOf(st.getMetrics().getBloodGlucose()),
                String.valueOf(st.getMetrics().getTotalCaloriesBurned()),
                String.valueOf(st.getMetrics().getSleepSession())
        );

        String system = """
                    You are a skincare and supplement safety explainer.
                    Write a short, consumer-friendly, personalized summary based ONLY on the provided user profile and ingredient analysis.
                    DO NOT invent ingredients or user data. No medical diagnosis. Keep it practical and gentle.
                    Output MUST be a single JSON object with this schema only:
                    { "summary": "string" }
                    No markdown, no extra fields, no text outside JSON.
                    Style: Korean, concise (2~5 sentences), friendly, actionable, avoid fearmongering.
                    If there are potentially irritating or cautious ingredients, mention them briefly (1~3 examples).
                    If there are beneficial/moisturizing/barrier-support ingredients, mention them briefly (1~3 examples).
                    If the user seems sensitive (e.g., ACNE/REDNESS/ATOPIC, 지성/수부지), suggest a simple patch-test tip.
                    If matchRate is 0, focus on the overall safe/neutral impression and general first-use tip.
                """;

        String user = """
                    User profile:
                    - SkinType: %s
                    - SkinConcerns (codes or labels): %s
                    - HealthConcerns (codes or labels): %s
                    - HairConcerns (codes or labels): %s
                    - Metrics: %s
                
                    Ingredient analysis:
                    - totalCount: %d
                    - matchRate: %.1f
                    - risky: %s
                    - caution: %s
                    - safe: %s
                
                    Return JSON only as: { "summary": "..." }
                """.formatted(
                st.getSkinTypeName() == null ? "" : st.getSkinTypeName(),
                concernsStr, healthStr, hairStr, metricsStr,
                allIngredients.size(), matchRate,
                risky.toString(), caution.toString(), // 간단히 문자열로
                safe.stream().limit(5).toList().toString()
        );

        try {
            String rsp = chat.prompt().system(system).user(user).call().content();
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = om.readTree(rsp);
            String summary = root.path("summary").asText(null);
            if (summary == null || summary.isBlank()) {
                return fallbackSummary(risky, caution, safe, st);
            }
            return summary.trim();
        } catch (Exception e) {
            return fallbackSummary(risky, caution, safe, st);
        }
    }

    private String fallbackSummary(
            List<String> risky, List<String> caution, List<String> safe, MemberStatusResponse st
    ) {
        StringBuilder sb = new StringBuilder("전반적으로 무난한 구성입니다. ");
        if (!risky.isEmpty()) {
            sb.append("자극 가능 성분: ").append(String.join(", ", risky)).append(". ");
        }
        if (!caution.isEmpty()) {
            sb.append("주의 성분: ").append(String.join(", ", caution)).append(". ");
        }
        if (st.getSkinConcerns() != null &&
                (st.getSkinConcerns().contains("ACNE") || st.getSkinConcerns().contains("REDNESS")
                        || st.getSkinConcerns().contains("ATOPIC"))) {
            sb.append("예민 피부라면 먼저 소량으로 패치 테스트해 보세요.");
        } else {
            sb.append("처음 사용 시에는 소량으로 테스트해 보세요.");
        }
        return sb.toString().trim();
    }

}