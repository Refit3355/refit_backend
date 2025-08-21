package com.refit.app.domain.me.mapper;

import com.refit.app.domain.me.dto.CombinationItemDto;
import com.refit.app.domain.me.dto.MyOrderItemDto;
import com.refit.app.domain.me.dto.response.CombinationResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeMapper {
    // 회원의 주문 내역
    List<MyOrderItemDto> findOrdersByMemberId(@Param("memberId") Long memberId);

    // 회원의 조합 목록
    List<CombinationResponse> findCombinationsByMember(@Param("memberId") Long memberId);

    // 회원의 상품 목록
    List<CombinationItemDto> findCombinationItems(@Param("combinationId") Long combinationId);
}
