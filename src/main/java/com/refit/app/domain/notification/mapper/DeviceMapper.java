package com.refit.app.domain.notification.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceMapper {

    int updateDeviceByKey(@Param("memberId") Long memberId,
            @Param("deviceId") String deviceId,
            @Param("platform") String platform,
            @Param("fcmToken") String fcmToken);

    int insertDevice(@Param("memberId") Long memberId,
            @Param("deviceId") String deviceId,
            @Param("platform") String platform,
            @Param("fcmToken") String fcmToken);

    int deleteDeviceByDeviceId(@Param("memberId") Long memberId,
            @Param("deviceId") String deviceId);

    List<String> selectActiveTokensByMemberId(@Param("memberId") Long memberId);

    void deactivateByToken(@Param("token") String token);

    void markSuccessByToken(@Param("token") String token);

    // 오래된 비활성 토큰 정리
    int deleteStale();
}
