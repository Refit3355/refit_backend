package com.refit.app.domain.memberProduct.service;

import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.dto.response.MemberProductDetailResponse;
import com.refit.app.domain.memberProduct.mapper.MemberProductMapper;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.memberProduct.model.UsageStatus;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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
                product.getBhType(),
                product.getCategoryId()
        );
    }

    @Override
    @Transactional
    public void createCustom(Long memberId, MemberProductCreateRequest req) {
        Integer bhType = toBhType(req.getType());
        LocalDate startDate = req.getStartDate();
        Integer recommendedDays = req.getRecommendedPeriodDays();
        List<Long> effectIds = (req.getEffect() == null) ? Collections.emptyList() : req.getEffect();

        Long memberProductId = memberProductMapper.insertMemberProductWithEffects(
                memberId,
                null,
                startDate,
                recommendedDays,
                1,
                req.getProductName(),
                req.getBrandName(),
                bhType,
                req.getCategoryId(),
                effectIds
        );
    }

    private int toBhType(ProductType type) {
        if (type == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "부적절한 인수 값 type: " + type.toString());
        }
        return type.getCode();
    }

    private Integer toStatusCodeNullable(UsageStatus statusOrNull) {
        return (statusOrNull == null) ? null : statusOrNull.getCode();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberProductDetailResponse> getMemberProducts(Long memberId,
            ProductType type,
            UsageStatus status) {
        final int bhType = toBhType(type);
        final Integer statusCode = toStatusCodeNullable(status);
        return memberProductMapper.selectMemberProducts(memberId, bhType, statusCode);
    }

    @Override
    @Transactional
    public void deleteMemberProduct(Long memberId, Long memberProductId) {
        int updated = memberProductMapper.softDeleteMemberProduct(memberId, memberProductId);
        if (updated == 0) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "삭제 대상이 없거나 권한이 없습니다.");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long memberId, Long memberProductId, UsageStatus status) {
        if (status == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "status 는 필수입니다.");
        }
        int updated;
        switch (status) {
            case USING -> updated = memberProductMapper.markUsing(memberId, memberProductId);
            case COMPLETED -> updated = memberProductMapper.markCompleted(memberId, memberProductId);
            case DELETED -> throw new RefitException(
                    ErrorCode.ILLEGAL_ARGUMENT,
                    "삭제는 DELETE API를 사용하세요. status 변경 API는 using/completed만 허용합니다."
            );
            default -> throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "지원하지 않는 상태: " + status);
        }
        if (updated == 0) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "대상이 없거나 상태 변경 불가합니다.");
        }
    }
}
