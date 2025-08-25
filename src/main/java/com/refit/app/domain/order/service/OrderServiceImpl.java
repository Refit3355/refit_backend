package com.refit.app.domain.order.service;

import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.response.UpdateOrderStatusResponse;
import com.refit.app.domain.order.mapper.OrderMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private int toBhType(ProductType type) {
        if (type == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "부적절한 인수 값 type: " + type.toString());
        }
        return type.getCode();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDto> getUnregisteredOrderItems(Long memberId, ProductType type) {
        final int bhType = toBhType(type);
        return orderMapper.selectUnregisteredOrderItems(memberId, bhType);
    }

    @Override
    @Transactional
    public UpdateOrderStatusResponse updateOrderItemStatus(Long memberId, Long orderItemId, int status) {

        long res = orderMapper.updateOrderItemStatus(memberId, orderItemId, status);

        String message = "교환/반품 신청에 실패하였습니다.";
        if (res == 1 && status == 4) message = "교환 신청이 완료되었습니다.";
        else if (res == 1 && status == 6) message = "반품 신청이 완료되었습니다.";

        return UpdateOrderStatusResponse.builder().message(message).build();
    }
}
