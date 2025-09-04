package com.refit.app.domain.notification.service;

import com.refit.app.domain.notification.mapper.DeviceMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    @Override
    @Transactional
    public void register(Long memberId, String platform, String fcmToken, String deviceId) {
        deviceMapper.upsertDevice(memberId, platform, fcmToken, deviceId);
    }

    @Override
    @Transactional
    public void deleteByDeviceId(Long memberId, String deviceId) {
        deviceMapper.deleteDeviceByDeviceId(memberId, deviceId);
    }

    @Override
    public List<String> findFcmTokensByMemberId(Long memberId) {
        return deviceMapper.selectActiveTokensByMemberId(memberId);
    }
}
