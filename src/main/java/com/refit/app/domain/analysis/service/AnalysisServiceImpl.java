package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.dto.AnalysisHairConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthInfoDto;
import com.refit.app.domain.analysis.dto.AnalysisSkinConcernDto;
import com.refit.app.domain.analysis.dto.response.MemberStatusResponse;
import com.refit.app.domain.analysis.mapper.AnalysisMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

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

    private boolean on(Integer v) {
        return v != null && v == 1;
    }

}
