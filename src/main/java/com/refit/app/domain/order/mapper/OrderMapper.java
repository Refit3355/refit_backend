package com.refit.app.domain.order.mapper;

import com.refit.app.domain.order.dto.OrderItemDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {

    List<OrderItemDto> selectUnregisteredOrderItems(
            @Param("memberId") Long memberId,
            @Param("bhType") Integer bhType);
}
