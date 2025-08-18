package com.refit.app.domain.memberProduct.service;

import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.mapper.MemberProductMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberProductServiceImpl implements MemberProductService {

    private final MemberProductMapper memberProductMapper;

    @Override
    @Transactional
    public void createFromProduct(Long memberId, Long productId) {
        // 1. 상품 기본 정보 조회
        ProductSimpleRow product = memberProductMapper.findProductSimple(productId);
        if (product == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "상품이 존재하지 않습니다. id=" + productId);
        }

        // 2. INSERT 파라미터 구성
        LocalDate startDate = LocalDate.now();
        Integer recommendedExpirationDate = product.getRecommendedPeriod();

        // 3. INSERT
        memberProductMapper.insertMemberProduct(
                memberId,
                productId,
                startDate,
                recommendedExpirationDate,
                1, // USAGE_STATUS=1 (사용중)
                product.getProductName(),
                product.getBrandName(),
                product.getBhType()
        );
    }

    @Override
    @Transactional
    public void createCustom(Long memberId, MemberProductCreateRequest req) {
        Integer type = typeCheck(req.getType());

        LocalDate startDate = req.getStartDate(); // YYYY-MM-DD
        Integer recommendedDays = req.getRecommendedPeriodDays();

        memberProductMapper.insertMemberProduct(
                memberId,
                null, // PRODUCT_ID 없음
                startDate,
                recommendedDays,
                1, // 사용중
                req.getProductName(),
                req.getBrandName(),
                type
        );
    }

    private int typeCheck(String type){
        if ("beauty".equalsIgnoreCase(type)) return 0;
        else if ("health".equalsIgnoreCase(type)) return 1;
        else {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT,
                    "타입 값은 'health' 또는 'beauty'여야 합니다.");
        }
    }
}
