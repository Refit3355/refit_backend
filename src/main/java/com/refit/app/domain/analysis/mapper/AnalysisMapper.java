package com.refit.app.domain.analysis.mapper;

import com.refit.app.domain.analysis.dto.AnalysisHairConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthConcernDto;
import com.refit.app.domain.analysis.dto.AnalysisHealthInfoDto;
import com.refit.app.domain.analysis.dto.AnalysisSkinConcernDto;
import com.refit.app.domain.analysis.dto.IngredientRule;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnalysisMapper {

    AnalysisHealthInfoDto selectHealthInfo(@Param("memberId") Long memberId);

    AnalysisSkinConcernDto selectSkinConcern(@Param("memberId") Long memberId);

    AnalysisHealthConcernDto selectHealthConcern(@Param("memberId") Long memberId);

    AnalysisHairConcernDto selectHairConcern(@Param("memberId") Long memberId);

    List<IngredientRule> selectByNames(@Param("names") Collection<String> names);
  
    String selectMemberNickname(@Param("memberId") Long memberId);
}
