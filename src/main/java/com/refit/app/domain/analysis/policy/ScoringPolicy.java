package com.refit.app.domain.analysis.policy;

public class ScoringPolicy {

    public record Profile(
            boolean sensitiveSkin, boolean acneProne, boolean atopic, boolean innerDryness,
            boolean redness
    ) {

    }

    /**
     * 매칭률 산식(간단형): - 기본 100점에서 시작 - 안전 성분(safe)은 점수에 영향 없음 (감점 X) - 주의(caution) / 위험(danger) "비율"에
     * 따라 감점 - 피부 프로필(민감/여드름/아토피/홍조/속건성)은 감점 가중치(factor)만 증가
     * <p>
     * score = 100 - penalty penalty = (wDanger * fracDanger + wCaution * fracCaution) * 100 *
     * factor
     * <p>
     * 권장 기본값: - wDanger = 0.70 (위험 성분 가중치) - wCaution = 0.30 (주의 성분 가중치) - factor:
     * 민감/여드름/아토피/홍조/속건성에 따라 +가중 (최대 2.0 배)
     */
    public static int computeMatchRate(
            int totalExtracted, int matchedCount, // matchedCount는 현재 미사용(요청대로 safe는 보너스/페널티 없음)
            int safeCount, int cautionCount, int dangerCount,
            Profile profile
    ) {
        // 텍스트가 전혀 없거나 파싱 실패 시 중립값
        if (totalExtracted <= 0) {
            return 50;
        }

        // 비율(0~1)
        double fracCaution = clamp01((double) cautionCount / (double) totalExtracted);
        double fracDanger = clamp01((double) dangerCount / (double) totalExtracted);

        // 가중치 (필요 시 조절)
        double wCaution = 0.30;
        double wDanger = 0.70;

        // 피부 프로필에 따른 감점 배수 (최대 2.0)
        double factor = 1.0;
        if (profile != null) {
            if (profile.sensitiveSkin()) {
                factor += 0.25;
            }
            if (profile.acneProne()) {
                factor += 0.15;
            }
            if (profile.atopic()) {
                factor += 0.15;
            }
            if (profile.redness()) {
                factor += 0.15;
            }
            if (profile.innerDryness()) {
                factor += 0.10;
            }
        }
        factor = Math.min(factor, 2.0);

        // 최종 감점
        double penalty = (wDanger * fracDanger + wCaution * fracCaution) * 100.0 * factor;

        // 스코어(0~100) 보정
        int score = (int) Math.round(100.0 - penalty);
        return clampInt(score, 0, 100);
    }

    // ------ helpers ------
    private static double clamp01(double v) {
        if (v < 0.0) {
            return 0.0;
        }
        if (v > 1.0) {
            return 1.0;
        }
        return v;
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
