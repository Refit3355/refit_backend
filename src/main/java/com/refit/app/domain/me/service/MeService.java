package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;

public interface MeService {

    RecentMyOrderResponse getRecentOrders(Long memberId);

}
