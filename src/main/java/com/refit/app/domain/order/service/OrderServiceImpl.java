package com.refit.app.domain.order.service;

import com.refit.app.domain.order.dto.MemberAddressRow;
import com.refit.app.domain.auth.mapper.MemberMapper;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.OrderInsertRow;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.request.DraftOrderRequest;
import com.refit.app.domain.order.dto.request.OrderLineItem;
import com.refit.app.domain.order.dto.response.DraftOrderResponse;
import com.refit.app.domain.order.dto.response.OrderItemSummary;
import com.refit.app.domain.order.dto.response.ShippingInfo;
import com.refit.app.domain.order.mapper.OrderMapper;
import com.refit.app.domain.order.dto.CartLineRow;
import com.refit.app.domain.order.dto.OrderItemInsertRow;
import com.refit.app.domain.order.dto.ProductSummaryRow;
import com.refit.app.domain.order.model.OrderSource;
import com.refit.app.domain.product.mapper.ProductMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final MemberMapper memberMapper;

    private static final long FREE_SHIPPING_THRESHOLD = 30_000L;
    private static final long BASE_DELIVERY_FEE = 3_000L;

    private int toBhType(ProductType type) {
        if (type == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "부적절한 인수 값 type: null");
        }
        return type.getCode();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDto> getUnregisteredOrderItems(Long memberId, ProductType type) {
        final int bhType = toBhType(type);
        return orderMapper.selectUnregisteredOrderItems(memberId, bhType);
    }

    @Transactional
    @Override
    public DraftOrderResponse createDraft(Long memberId, DraftOrderRequest req) {
        // 1) 라인 구성
        List<OrderItemSummary> items = buildItems(req, memberId);
        if (items.isEmpty()) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "주문 상품이 비어 있습니다.");
        }

        // 2) 금액 계산
        long goodsTotal = 0L;
        long discountTotal = 0L;

        for (OrderItemSummary it : items) {
            long original = it.getOriginalPrice();
            long rate = (it.getDiscountRate() == null) ? 0L : it.getDiscountRate();
            int qty = it.getQuantity();

            // 상품별 할인금액 (100원 미만 버림)
            long discountPerUnit = (original * rate / 100L) / 100L * 100L; // 내림 처리
            long discountForItem = discountPerUnit * qty;

            // 상품별 판매가
            long salePrice = original - discountPerUnit;

            // 합계 계산
            goodsTotal += salePrice * qty;
            discountTotal += discountForItem;
        }

        long deliveryFee = (goodsTotal >= FREE_SHIPPING_THRESHOLD) ? 0L : BASE_DELIVERY_FEE;
        long totalAmount = goodsTotal + deliveryFee;


        // 3) 주문명
        String orderSummary = (items.size() == 1)
                ? items.get(0).getProductName()
                : items.get(0).getProductName() + " 외 " + (items.size() - 1) + "건";

        // 4) 배송지 스냅샷
        MemberAddressRow addr = memberMapper.findShippingByMemberId(memberId);
        if (addr == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "회원 배송지 없음");

        ShippingInfo shipping = ShippingInfo.builder()
                .receiverName(addr.getMemberName())
                .phone(addr.getPhoneNumber())
                .roadAddress(addr.getRoadAddress())
                .detailAddress(addr.getDetailAddress())
                .zipcode(addr.getZipcode())
                .memo(addr.getDeliveryNote())
                .build();

        // 5) 주문번호
        String orderCode = generateOrderCode(memberId);

        // 6) ORDERS insert
        LocalDateTime now = LocalDateTime.now();

        OrderInsertRow orderRow = OrderInsertRow.builder()
                .memberId(memberId)
                .orderCode(orderCode)
                .totalPrice(totalAmount)
                .goodsAmount(goodsTotal)
                .deliveryAddress(addr.getRoadAddress())
                .zipcode(addr.getZipcode())
                .detailAddress(addr.getDetailAddress())
                .roadAddress(addr.getRoadAddress())
                .orderStatus(0) // REQUESTED
                .deliveryFee(deliveryFee)
                .discount(discountTotal)
                .orderSummary(orderSummary)
                .createdBy(memberId)
                .updatedBy(memberId)
                .build();

        int affected = orderMapper.insertOrder(orderRow);
        // Oracle 드라이버가 SUCCESS_NO_INFO 등 음수를 반환할 수 있으므로 행수는 참고만.
        // 실질 성공 여부는 selectKey로 채워진 PK로 판단.
        if (orderRow.getOrderId() == null) {
            log.error("ORDERS insert failed, affected={}, row={}", affected, orderRow);
            throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "주문 생성 실패(orders)");
        }
        log.info("insertOrder affected={}, orderId={}", affected, orderRow.getOrderId());

        Long orderId = orderRow.getOrderId();

        // 7) ORDER_ITEM insert (정가/할인율/할인가 모두 기록)
        for (OrderItemSummary it : items) {
            OrderItemInsertRow row = new OrderItemInsertRow();
            long rate = (it.getDiscountRate() == null) ? 0L : it.getDiscountRate();
            long discountedPrice = (it.getPrice() * (100 - rate) / 100L) / 100L * 100L;
            row.setOrderId(orderId);
            row.setProductId(it.getProductId());
            row.setOrderStatus(0); // REQUESTED
            row.setItemCount(it.getQuantity());
            row.setOrgUnitPrice(it.getOriginalPrice());
            row.setDiscountRate(it.getDiscountRate());
            row.setItemPrice(discountedPrice); // 할인단가
            row.setLineAmount(discountedPrice * it.getQuantity());
            row.setProductName(it.getProductName());
            row.setBrandName(it.getBrandName());
            row.setThumbnailUrl(it.getThumbnailUrl());
            row.setMemberId(memberId);
            row.setCreatedBy(memberId);
            row.setUpdatedBy(memberId);

            int ai = orderMapper.insertOrderItem(row);
            if (row.getOrderItemId() == null) {
                log.error("ORDER_ITEM insert failed, affected={}, row={}", ai, row);
                throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "주문 생성 실패(order_item)");
            }
        }

        // 8) 응답
        return DraftOrderResponse.builder()
                .orderCode(orderCode)
                .orderId(orderId)
                .orderSummary(orderSummary)
                .totalAmount(totalAmount)
                .items(items)
                .shipping(shipping)
                .build();
    }


    private List<OrderItemSummary> buildItems(DraftOrderRequest req, Long memberId) {
        List<OrderItemSummary> list = new ArrayList<>();

        OrderSource src = req.getSource(); // enum
        if (src == OrderSource.DIRECT) {
            List<OrderLineItem> lines = req.getLines();
            if (lines == null || lines.isEmpty()) {
                throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "직구매 라인이 비어 있음");
            }
            for (OrderLineItem l : lines) {
                ProductSummaryRow p = productMapper.findSummaryById(l.getProductId());
                if (p == null) {
                    throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "상품 없음: " + l.getProductId());
                }
                list.add(OrderItemSummary.builder()
                        .productId(p.getId())
                        .originalPrice(p.getOriginalPrice())
                        .discountRate(p.getDiscountRate() == null ? null : Long.valueOf(p.getDiscountRate()))
                        .price(p.getDiscountedPrice())
                        .quantity(l.getQuantity())
                        .productName(p.getName())
                        .brandName(p.getBrandName())
                        .thumbnailUrl(p.getThumbnailUrl())
                        .build());
            }
        } else if (src == OrderSource.CART) {
            if (req.getCartItemIds() == null || req.getCartItemIds().isEmpty()) {
                throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "장바구니 항목이 비어 있음");
            }
            List<CartLineRow> rows = orderMapper.findCartLines(memberId, req.getCartItemIds());
            for (CartLineRow r : rows) {
                list.add(OrderItemSummary.builder()
                        .productId(r.getProductId())
                        .originalPrice(r.getOriginalPrice())
                        .discountRate(r.getDiscountRate() == null ? null : Long.valueOf(r.getDiscountRate()))
                        .price(r.getDiscountedPrice())
                        .quantity(r.getQuantity())
                        .productName(r.getProductName())
                        .brandName(r.getBrandName())
                        .thumbnailUrl(r.getThumbnailUrl())
                        .build());
            }
        } else {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "지원하지 않는 source: " + src);
        }

        return list;
    }

    private String generateOrderCode(Long memberId) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + ts + "-" + memberId + "-" + rand;
    }
}
