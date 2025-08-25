package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.response.CombinationsResponse;
import com.refit.app.domain.me.dto.response.ProfileImageSaveResponse;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MeService {

    RecentMyOrderResponse getRecentOrders(Long memberId);

    CombinationsResponse getMyCombinations(Long memberId);

    ProfileImageSaveResponse updateProfileImage(Long memberId, MultipartFile profileImage);
}
