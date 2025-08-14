package com.refit.app.auth.mapper;

import com.refit.app.auth.dto.HealthRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConcernMapper {

    int mergeHealth(@Param("memberId") Long memberId, @Param("h") HealthRequest h);
}
