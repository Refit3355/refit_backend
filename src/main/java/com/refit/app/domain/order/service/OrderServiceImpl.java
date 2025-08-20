package com.refit.app.domain.order.service;

import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.OrderItemDto;
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
}
