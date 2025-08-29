package com.refit.app.domain.auth.mapper;

import com.refit.app.domain.auth.dto.request.SamsungHealthSaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HealthInfoMapper {
    int existsByMemberId(@Param("memberId") Long memberId);
    void insertHealthInfo(@Param("memberId") Long memberId, @Param("req") SamsungHealthSaveRequest request);
    void updateHealthInfo(@Param("memberId") Long memberId, @Param("req")SamsungHealthSaveRequest request);
}
