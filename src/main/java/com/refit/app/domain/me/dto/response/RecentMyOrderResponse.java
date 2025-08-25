package com.refit.app.domain.me.dto.response;

import com.refit.app.domain.me.dto.MyOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentMyOrderResponse {
    private List<MyOrderDto> recentOrder;
}

