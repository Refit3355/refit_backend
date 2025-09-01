package com.refit.app.domain.analysis.policy;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class IngredientNormalizer {

    private static final Map<String, String> ALIAS = new HashMap<>();

    static {
        ALIAS.put("소듐하이알루로네이트", "히알루론산(소듐하이알루로네이트)");
        ALIAS.put("소듐 히알루로네이트", "히알루론산(소듐하이알루로네이트)");

        ALIAS.put("에틸헥실 메톡시신나메이트", "옥틸메톡시신나메이트(옥티놀레이트)");
        ALIAS.put("octocrylene", "옥토크릴렌");

        ALIAS.put("niacinamide", "나이아신아마이드");
        ALIAS.put("retinol", "레티놀(비타민 A 유도체)");
        ALIAS.put("salicylicacid", "살리실산(BHA)");
        ALIAS.put("betaine", "베타인");
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        s = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replace('（', '(').replace('）', ')')
                .replaceAll("\\s{2,}", " ");
        String key = s.toLowerCase().replaceAll("\\s", "");
        String mapped = ALIAS.get(key);
        if (mapped != null) {
            return mapped;
        }
        return s.replaceAll("[\\.:;]+$", "");
    }
}
