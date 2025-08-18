package com.refit.app.domain.memberProduct.service;

import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;

public interface MemberProductService {

    void createFromProduct(Long memberId, Long productId);

    void createCustom(Long memberId, MemberProductCreateRequest req);
}
