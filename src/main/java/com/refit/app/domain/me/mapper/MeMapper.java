package com.refit.app.domain.me.mapper;

import com.refit.app.domain.me.dto.MyOrderItemDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeMapper {
    List<MyOrderItemDto> findOrdersByMemberId(@Param("memberId") Long memberId);
}
