package com.refit.app.domain.order.mapper;

import com.refit.app.domain.order.dto.CartLineRow;
import com.refit.app.domain.order.dto.OrderInsertRow;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.OrderItemInsertRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {

    List<OrderItemDto> selectUnregisteredOrderItems(
            @Param("memberId") Long memberId,
            @Param("bhType") Integer bhType);

    int insertOrder(OrderInsertRow row);       // selectKey로 ORDER_ID 세팅
    int insertOrderItem(OrderItemInsertRow r); // selectKey로 ORDER_ITEM_ID 세팅

    List<CartLineRow> findCartLines(@Param("memberId") long memberId,
            @Param("cartItemIds") List<Long> cartItemIds);

    long updateOrderItemStatus(
            @Param("memberId") Long memberId,
            @Param("orderItemId") Long orderItemId,
            @Param("status") int status);
}
