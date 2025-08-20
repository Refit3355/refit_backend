package com.refit.app.domain.memberProduct.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MemberProductListResponse {
    private List<MemberProductDetailResponse> items;
    private int total;
}
