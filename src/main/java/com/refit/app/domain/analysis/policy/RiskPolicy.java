package com.refit.app.domain.analysis.policy;

import static java.util.Map.entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class RiskPolicy {

    private final ChatClient chat;
    private final ObjectMapper om = new ObjectMapper();

    public RiskPolicy(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    private static final Map<String, String> BASELINE_RISK = Map.of(
            "카페인", "medium",
            "알코올", "medium",
            "오메가3", "low",
            "비타민D", "low",
            "비타민E", "low",
            "EPA", "low",
            "DHA", "low"
    );

    private static final Map<String, Map<String, String>> OVERRIDE_BY_CONCERN = Map.ofEntries(
            // 기존
            entry("여드름/민감성", Map.of(
                    "알코올", "high",
                    "12-헥산다이올", "medium",                 // 보존/용제 → 자극 가능
                    "아보카도오일", "medium"                   // 코메도제닉 가능성
            )),
            entry("홍조", Map.of(
                    "알코올", "high",
                    "12-헥산다이올", "medium"
            )),
            entry("피지/블랙헤드", Map.of(
                    "아보카도오일", "medium",
                    "카프릴릭/카프릭트리글리세라이드", "low",
                    "3-오크틸도데칸올", "low"
            )),
            // 모발/두피는 필요시 추가
            entry("두피트러블", Map.of("알코올", "high")),
            entry("비듬/각질", Map.of("알코올", "medium")),

            // 건강 쪽 기존
            entry("수면/스트레스", Map.of("카페인", "high")),
            entry("혈액순환", Map.of("오메가3", "medium", "EPA", "medium", "DHA", "medium")),
            entry("면역력", Map.of("비타민E", "low", "비타민D", "low")),
            entry("장건강", Map.of("오메가3", "low")),
            entry("눈건강", Map.of("오메가3", "low", "DHA", "low")),
            entry("만성피로", Map.of("카페인", "medium")),
            entry("근력", Map.of("비타민D", "low"))
    );


    @Getter
    @AllArgsConstructor
    public static class RiskResult {

        private String risk;   // high|medium|low|none
        private String reason; // audit/debug
    }

    public RiskResult evaluate(String ingredient,
            List<String> skinConcerns,
            List<String> healthConcerns,
            List<String> hairConcerns,
            Map<String, Object> metrics
    ) {
        String risk = BASELINE_RISK.getOrDefault(ingredient, "none");
        StringBuilder why = new StringBuilder("baseline:" + risk);

        List<String> all = ConcernCatalog.mergeAllConcerns(skinConcerns, healthConcerns,
                hairConcerns, null);
        for (String c : all) {
            Map<String, String> table = OVERRIDE_BY_CONCERN.get(c);
            if (table != null && table.containsKey(ingredient)) {
                String orisk = table.get(ingredient);
                if (rank(orisk) > rank(risk)) {
                    risk = orisk;
                    why.append(", override(").append(c).append(":").append(orisk).append(")");
                }
            }
        }

        if (metrics != null) {
            Integer sleep = asInt(metrics.get("sleepSession"));
            if ("카페인".equals(ingredient) && sleep != null && sleep <= 5 && rank("high") > rank(
                    risk)) {
                risk = "high";
                why.append(", metric(sleep<=5->high)");
            }
            Integer bp = asInt(metrics.get("bloodPressure"));
            if ("카페인".equals(ingredient) && bp != null && bp >= 140 && rank("high") > rank(risk)) {
                risk = "high";
                why.append(", metric(bp>=140->high)");
            }
        }

        if (!BASELINE_RISK.containsKey(ingredient)) {
            RiskResult llm = llmJudge(ingredient, all, metrics);
            if (rank(llm.risk) >= rank("medium") && rank(llm.risk) > rank(risk)) {
                risk = llm.risk;
                why.append(", llm:").append(llm.reason);
            }
        }

        return new RiskResult(risk, why.toString());
    }

    private int rank(String r) {
        return switch (r) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private RiskResult llmJudge(String ingredient, List<String> concerns,
            Map<String, Object> metrics) {
        String system = """
                    You are a medical safety analyst.
                    Classify the risk of an ingredient for a specific user profile.
                    You must answer in a single JSON object only.
                    No explanations, no markdown, no text outside JSON.
                    If unknown, still return {"risk":"none","reason":"unknown"}.
                """;

        String user = """
                    User concerns: %s
                    User metrics: %s
                    Ingredient: %s
                    Respond in JSON with the schema:
                    { "risk": "high|medium|low|none", "reason": "string" }
                """.formatted(
                ConcernCatalog.toPromptString(concerns),
                (metrics == null || metrics.isEmpty()) ? "none" : metrics.toString(),
                ingredient
        );

        try {
            String rsp = chat.prompt().system(system).user(user).call().content();
            JsonNode root = om.readTree(rsp);
            return new RiskResult(
                    root.path("risk").asText("none"),
                    root.path("reason").asText("llm")
            );
        } catch (Exception e) {
            return new RiskResult("none", "parse_error");
        }
    }
}