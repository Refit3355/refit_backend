package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.response.CombinationResponse;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import java.util.List;

public interface MeService {

    RecentMyOrderResponse getRecentOrders(Long memberId);

    List<CombinationResponse> getMyCombinations(Long memberId);

}
