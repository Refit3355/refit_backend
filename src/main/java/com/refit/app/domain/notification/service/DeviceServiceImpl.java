package com.refit.app.domain.notification.service;

import com.refit.app.domain.notification.mapper.DeviceMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    @Override
    @Transactional
    public void register(Long memberId, String platform, String fcmToken, String deviceId) {
        // 1) UPDATE 먼저
        int updated = deviceMapper.updateDeviceByKey(memberId, deviceId, platform, fcmToken);
        if (updated > 0) return;

        // 2) 없다면 INSERT 시도
        try {
            deviceMapper.insertDevice(memberId, deviceId, platform, fcmToken);
        } catch (DuplicateKeyException e) {
            // 3) 경쟁 상황에서 다른 트랜잭션이 먼저 넣은 경우 → 최종 UPDATE로 정합성 회복
            deviceMapper.updateDeviceByKey(memberId, deviceId, platform, fcmToken);
        }
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
