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

    int deleteDeviceByToken(@Param("memberId") Long memberId,
            @Param("fcmToken") String fcmToken);

    List<String> selectTokensByMemberId(@Param("memberId") Long memberId);

}
