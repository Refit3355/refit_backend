package com.refit.app.domain.memberProduct.mapper;

import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberProductMapper {

    ProductSimpleRow findProductSimple(@Param("productId") Long productId);

    void insertMemberProduct(@Param("memberId") Long memberId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("recommendedExpirationDate") Integer recommendedExpirationDate,
            @Param("usageStatus") int usageStatus,
            @Param("productName") String productName,
            @Param("brandName") String brandName,
            @Param("type") Integer type);

}
