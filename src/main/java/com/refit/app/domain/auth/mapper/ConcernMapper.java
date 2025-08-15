package com.refit.app.domain.auth.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.refit.app.domain.auth.dto.HealthRequest;

@Mapper
public interface ConcernMapper {

    int mergeHealth(@Param("memberId") Long memberId, @Param("h") HealthRequest h);
}
