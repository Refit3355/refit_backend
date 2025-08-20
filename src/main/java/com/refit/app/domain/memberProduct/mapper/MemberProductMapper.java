package com.refit.app.domain.memberProduct.mapper;

import com.refit.app.domain.memberProduct.dto.MetaRow;
import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import com.refit.app.domain.memberProduct.dto.response.MemberProductDetailResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberProductMapper {

    ProductSimpleRow findProductSimple(@Param("productId") Long productId);

    void insertMemberProduct(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("recommendedExpirationDate") Integer recommendedExpirationDate,
            @Param("usageStatus") int usageStatus,
            @Param("productName") String productName,
            @Param("brandName") String brandName,
            @Param("type") Integer type,
            @Param("categoryId") Long categoryId);

    Long insertMemberProductWithEffects(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("recommendedExpirationDate") Integer recommendedExpirationDate,
            @Param("usageStatus") int usageStatus,
            @Param("productName") String productName,
            @Param("brandName") String brandName,
            @Param("type") Integer type,
            @Param("categoryId") Long categoryId,
            @Param("effectIds") List<Long> effectIds
    );

    List<MemberProductDetailResponse> selectMemberProducts(
            @Param("memberId") Long memberId,
            @Param("bhType") int bhType,
            @Param("statusCode") Integer statusCode);

    int softDeleteMemberProduct(
            @Param("memberId") Long memberId,
            @Param("memberProductId") Long memberProductId);

    int markUsing(@Param("memberId") Long memberId, @Param("memberProductId") Long memberProductId);

    int markCompleted(@Param("memberId") Long memberId, @Param("memberProductId") Long memberProductId);

    MetaRow findMemberProductMeta(
            @Param("memberId") Long memberId,
            @Param("memberProductId") Long memberProductId);

    int updateInAppMemberProduct(
            @Param("memberId") Long memberId,
            @Param("memberProductId") Long memberProductId,
            @Param("recommendedPeriod") Integer recommendedPeriod,
            @Param("startDate") String startDate);

    int updateExternalMemberProduct(
            @Param("memberId") Long memberId,
            @Param("memberProductId") Long memberProductId,
            @Param("productName") String productName,
            @Param("brandName") String brandName,
            @Param("recommendedPeriod") Integer recommendedPeriod,
            @Param("startDate") String startDate,
            @Param("categoryId") Long categoryId);

    int deleteAllEffects(@Param("memberProductId") Long memberProductId);

    int insertEffect(@Param("memberId") Long memberId,
            @Param("memberProductId") Long memberProductId,
            @Param("effectId") Long effectId);

}
