package com.refit.app.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.refit.app.domain.auth.dto.request.HairRequest;
import com.refit.app.domain.auth.dto.request.HealthRequest;
import com.refit.app.domain.auth.dto.request.SkinRequest;

@Mapper
public interface ConcernMapper {

	void mergeHealth(@Param("memberId") Long memberId, @Param("h") HealthRequest h);

	void mergeHair(@Param("memberId") Long memberId, @Param("h") HairRequest h);

	void mergeSkin(@Param("memberId") Long memberId, @Param("s") SkinRequest s);
}
