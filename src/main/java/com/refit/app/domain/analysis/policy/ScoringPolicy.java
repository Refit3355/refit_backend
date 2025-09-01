package com.refit.app.domain.analysis.policy;

public class ScoringPolicy {

    public record Profile(
            boolean sensitiveSkin, boolean acneProne, boolean atopic, boolean innerDryness,
            boolean redness
    ) {

    }

    public static int computeMatchRate(
            int totalExtracted, int matchedCount,
            int safeCount, int cautionCount, int dangerCount,
            Profile profile
    ) {
        if (totalExtracted <= 0) {
            return 50;
        }
        double base = ((double) matchedCount / totalExtracted) * 70.0;
        double bonus = Math.min(10.0, safeCount * 1.5);
        double penalty = cautionCount * 2.0 + dangerCount * 6.0;

        double factor = 1.0;
        if (profile.sensitiveSkin()) {
            factor += 0.3;
        }
        if (profile.acneProne()) {
            factor += 0.2;
        }
        if (profile.atopic()) {
            factor += 0.2;
        }
        if (profile.redness()) {
            factor += 0.2;
        }

        penalty = Math.min(penalty * factor, 40.0);
        int score = (int) Math.round(Math.max(0, Math.min(100, base + bonus - penalty)));
        return score;
    }
}
