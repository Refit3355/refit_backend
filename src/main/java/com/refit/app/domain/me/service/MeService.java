package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.response.MyCombinationResponse;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import java.util.List;

public interface MeService {

    RecentMyOrderResponse getRecentOrders(Long memberId);

    List<MyCombinationResponse> getMyCombinations(Long memberId);

}
