package com.refit.app.domain.auth.mapper;

import com.refit.app.domain.auth.dto.HairInfoDto;
import com.refit.app.domain.auth.dto.HealthInfoDto;
import com.refit.app.domain.auth.dto.SkinInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConcernMapper {

    void mergeHealth(@Param("memberId") Long memberId, @Param("h") HealthInfoDto h);

    void mergeHair(@Param("memberId") Long memberId, @Param("h") HairInfoDto h);

    void mergeSkin(@Param("memberId") Long memberId, @Param("s") SkinInfoDto s);
}
