package com.refit.app.domain.notification.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceMapper {

    int upsertDevice(@Param("memberId") Long memberId,
            @Param("platform") String platform,
            @Param("fcmToken") String fcmToken,
            @Param("deviceId") String deviceId);

    int deleteDeviceByDeviceId(@Param("memberId") Long memberId,
            @Param("deviceId") String deviceId);

    List<String> selectTokensByMemberId(@Param("memberId") Long memberId);

}
