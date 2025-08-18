package com.refit.app.domain.memberProduct.service;

import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.dto.response.MemberProductDetailResponse;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.memberProduct.model.UsageStatus;
import java.util.List;
import java.util.Map;

public interface MemberProductService {

    void createFromProduct(Long memberId, Long productId);

    void createCustom(Long memberId, MemberProductCreateRequest req);

    List<MemberProductDetailResponse> getMemberProducts(Long memberId, ProductType type, UsageStatus status);
}
