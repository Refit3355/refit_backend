package com.refit.app.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.notification.service.NotificationTriggerService;
import com.refit.app.domain.payment.dto.OrderItemRowDto;
import com.refit.app.domain.payment.dto.OrderRowDto;
import com.refit.app.domain.payment.dto.PaymentCancelRowDto;
import com.refit.app.domain.payment.dto.VaIssuedUpdateParam;
import com.refit.app.domain.payment.dto.response.ConfirmPaymentItemDto;
import com.refit.app.domain.payment.dto.PaymentRowDto;
import com.refit.app.domain.payment.dto.request.ConfirmPaymentRequest;
import com.refit.app.domain.payment.dto.request.PartialCancelRequest;
import com.refit.app.domain.payment.dto.response.ConfirmPaymentResponse;
import com.refit.app.domain.payment.dto.response.PartialCancelResponse;
import com.refit.app.domain.payment.mapper.PaymentMapper;
import com.refit.app.domain.product.mapper.ProductMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final WebClient tossWebClient;    // TossClientConfig 에서 BasicAuth(secretKey:) 세팅
    private final PaymentMapper paymentMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper om;

    private final PaymentCancelLogService cancelLogService;
    private final NotificationTriggerService notificationTriggerService;

    @Value("${toss.api-base}") String tossBase;
    private static final long FREE_SHIPPING_THRESHOLD = 30_000L;
    private static final long BASE_DELIVERY_FEE = 3_000L;

    public PaymentServiceImpl(
            @Qualifier("tossWebClient") WebClient tossWebClient,
            PaymentMapper paymentMapper,
            ObjectMapper objectMapper,
            ProductMapper productMapper,
            PaymentCancelLogService cancelLogService,
            NotificationTriggerService notificationTriggerService
    ) {
        this.tossWebClient = tossWebClient;
        this.paymentMapper = paymentMapper;
        this.om = objectMapper;
        this.productMapper = productMapper;
        this.cancelLogService = cancelLogService;
        this.notificationTriggerService = notificationTriggerService;
    }

    @Override
    public ConfirmPaymentResponse confirm(ConfirmPaymentRequest req, Long memberId) {
        // 1) 주문 잠금 + 금액 검증
        OrderRowDto order = validateAndLockOrder(req);

        // 2) 토스 승인 호출
        Map<?,?> paymentObj = callTossConfirm(req);

        // 3) 응답 파싱
        ParsedPayment parsed = parsePaymentResponse(paymentObj, req);

        // 4) PAYMENT insert (paymentId 확보)
        PaymentRowDto payRow = insertPaymentRow(order.getOrderId(), req.getOrderId(), parsed);

        // 5) 가상계좌 필드 저장
        if (parsed.va != null) {
            persistVirtualAccountFields(payRow.getPaymentId(), parsed.va, paymentObj);
        }

        // 6) VA 결제면 입금대기 처리 + 응답 빌드 후 반환
        if (parsed.isVirtualAccount) {
            return handleVirtualAccountFlow(order, payRow.getPaymentId(), parsed, paymentObj);
        }

        // 7) 일반결제 승인 후처리(주문/아이템/결제 업데이트, 재고 차감, 알림)
        finalizeApproval(order.getOrderId(), payRow.getPaymentId(),
                parsed.balanceAmount, parsed.receiptUrl, paymentObj, memberId, order.getOrderSummary());

        // 8) 승인 응답 빌드
        List<OrderItemRowDto> orderItems = paymentMapper.findOrderItems(order.getOrderId());
        return buildApprovedResponse(order, parsed, payRow.getPaymentId(), orderItems);
    }

    private OrderRowDto validateAndLockOrder(ConfirmPaymentRequest req) {
        String orderCode = req.getOrderId();
        OrderRowDto order = paymentMapper.findOrderForUpdate(orderCode);
        if (order == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "해당 주문 없음");
        if (!Objects.equals(order.getTotalPrice(), req.getAmount())) {
            throw new RefitException(ErrorCode.ORDER_AMOUNT_MISMATCH, "금액 불일치");
        }
        return order;
    }

    @SuppressWarnings("unchecked")
    private Map<?,?> callTossConfirm(ConfirmPaymentRequest req) {
        Map<String,Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", req.getOrderId(),
                "amount", req.getAmount()
        );
        return tossWebClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private static final class ParsedPayment {
        String paymentKey;
        String method;
        String currency;
        long totalAmount;
        long balanceAmount;
        String receiptUrl;
        Map<String,Object> va;      // virtualAccount map or null
        boolean isVirtualAccount;
    }

    @SuppressWarnings("unchecked")
    private ParsedPayment parsePaymentResponse(Map<?,?> paymentObj, ConfirmPaymentRequest req) {
        ParsedPayment p = new ParsedPayment();
        p.paymentKey    = Objects.toString(paymentObj.get("paymentKey"), null);
        p.method        = Objects.toString(paymentObj.get("method"), null);
        p.currency      = Objects.toString(paymentObj.get("currency"), "KRW");
        p.totalAmount   = ((Number) paymentObj.get("totalAmount")).longValue();
        p.balanceAmount = ((Number) paymentObj.get("balanceAmount")).longValue();
        p.receiptUrl    = null;
        Object receiptObj = paymentObj.get("receipt");
        if (receiptObj instanceof Map<?,?> r) {
            p.receiptUrl = Objects.toString(r.get("url"), null);
        }

        p.va = (Map<String, Object>) paymentObj.get("virtualAccount");
        p.isVirtualAccount =
                p.va != null
                        || "VIRTUAL_ACCOUNT".equalsIgnoreCase(req.getMethod())
                        || "VIRTUAL_ACCOUNT".equalsIgnoreCase(p.method)
                        || "가상계좌".equals(p.method);

        return p;
    }

    private PaymentRowDto insertPaymentRow(Long orderId, String orderCode, ParsedPayment p) {
        int status = "VIRTUAL_ACCOUNT".equalsIgnoreCase(p.method) ? 0 : 1; // VA는 READY/REQUESTED
        PaymentRowDto row = PaymentRowDto.builder()
                .orderId(orderId)
                .orderCode(orderCode)
                .paymentKey(p.paymentKey)
                .method(p.method)
                .currency(p.currency == null || p.currency.isBlank() ? "KRW" : p.currency)
                .totalAmount(p.totalAmount)
                .balanceAmount(p.balanceAmount)
                .status(status)
                .build();
        paymentMapper.insertPayment(row);
        return row;
    }

    private void persistVirtualAccountFields(Long paymentId, Map<String,Object> va, Map<?,?> paymentObj) {
        paymentMapper.updateVirtualAccountFields(
                paymentId,
                Objects.toString(va.get("accountNumber"), null),
                Objects.toString(va.get("bankCode"), null),
                Objects.toString(va.get("accountType"), null),
                Objects.toString(va.get("customerName"), null),
                Objects.toString(va.get("depositorName"), null),
                parseTs(Objects.toString(va.get("dueDate"), null)),
                Boolean.TRUE.equals(va.get("expired")) ? 1 : 0,
                Objects.toString(va.get("settlementStatus"), null),
                Objects.toString(va.get("refundStatus"), null),
                Objects.toString(paymentObj.get("secret"), null)
        );
    }

    private ConfirmPaymentResponse handleVirtualAccountFlow(
            OrderRowDto order, Long paymentId, ParsedPayment p, Map<?,?> paymentObj) {

        // 주문/아이템 상태를 입금대기(12)
        paymentMapper.updateOrderToDepositWaiting(order.getOrderId());
        paymentMapper.updateOrderItemsStatusByOrderId(order.getOrderId(), 12);

        // PAYMENT의 VA 발급 필드 업데이트
        paymentMapper.updatePaymentOnVaIssued(
                VaIssuedUpdateParam.builder()
                        .paymentId(paymentId)
                        .status(12)
                        .vaAccountNo(Objects.toString(p.va.get("accountNumber"), null))
                        .vaBankCode(Objects.toString(p.va.get("bankCode"), null))
                        .vaAccountType(Objects.toString(p.va.get("accountType"), null))
                        .vaCustomerName(Objects.toString(p.va.get("customerName"), null))
                        .vaDepositorName(Objects.toString(p.va.get("depositorName"), null))
                        .vaDueDate(parseTs(Objects.toString(p.va.get("dueDate"), null)))
                        .vaSecret(Objects.toString(paymentObj.get("secret"), null))
                        .rawJson(toJson(paymentObj))
                        .build()
        );

        // 응답을 위한 아이템 목록
        List<OrderItemRowDto> orderItems = paymentMapper.findOrderItems(order.getOrderId());
        var itemsForResponse = orderItems.stream().map(it -> ConfirmPaymentItemDto.builder()
                .productId(it.getProductId())
                .brandName(safe(it.getBrandName()))
                .productName(safe(it.getProductName()))
                .price(nz(it.getItemPrice()))
                .originalPrice(nzOr(it.getOrgUnitPrice(), nz(it.getItemPrice())))
                .quantity(it.getItemCount())
                .thumbnailUrl(safe(it.getThumbnailUrl()))
                .build()).toList();

        String firstThumb = itemsForResponse.isEmpty() ? null : itemsForResponse.get(0).getThumbnailUrl();
        int totalQty = itemsForResponse.stream().mapToInt(ConfirmPaymentItemDto::getQuantity).sum();

        return ConfirmPaymentResponse.builder()
                .paymentId(paymentId)
                .paymentKey(p.paymentKey)
                .totalAmount(p.totalAmount)
                .status("WAITING_FOR_DEPOSIT")
                .receiptUrl(p.receiptUrl)
                .orderPk(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderName(order.getOrderSummary())
                .method("VIRTUAL_ACCOUNT")
                .firstItemThumb(firstThumb)
                .itemCount(totalQty)
                .items(itemsForResponse)
                .vaAccountNo(Objects.toString(p.va.get("accountNumber"), null))
                .vaBankCode(Objects.toString(p.va.get("bankCode"), null))
                .vaDueDate(Objects.toString(p.va.get("dueDate"), null))
                .vaDepositorName(Objects.toString(p.va.get("depositorName"), null))
                .build();
    }

    private void finalizeApproval(Long orderId,
            Long paymentId,
            long balanceAmount,
            String receiptUrl,
            Map<?,?> paymentObj,
            Long memberId,
            String orderSummary) {

        // 주문/아이템 승인
        paymentMapper.markOrderPaid(orderId);
        paymentMapper.updateStatusToApprovedByOrderId(orderId);

        // PAYMENT 승인정보 저장
        paymentMapper.updatePaymentOnApproved(
                paymentId, balanceAmount, 1, receiptUrl, toJson(paymentObj)
        );

        // 재고 차감
        decreaseStocks(orderId);

        // 알림
        notificationTriggerService.notifyPaymentCompleted(memberId, orderId,
                orderSummary + "의 결제가 완료되었습니다.");
    }

    private void decreaseStocks(Long orderId) {
        List<OrderItemRowDto> orderItems = paymentMapper.findOrderItems(orderId);
        orderItems.sort(Comparator.comparing(OrderItemRowDto::getProductId)); // 교착 방지

        for (OrderItemRowDto it : orderItems) {
            Long productId = it.getProductId();
            int qty = it.getItemCount();
            if (qty <= 0) {
                throw new RefitException(ErrorCode.OUT_OF_STOCK, "잘못된 수량: " + qty);
            }
            Integer stock = productMapper.selectStockForUpdate(productId);
            if (stock == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "상품 없음: " + productId);
            if (stock < qty) throw new RefitException(ErrorCode.OUT_OF_STOCK,
                    "재고 부족: productId=" + productId + ", stock=" + stock + ", need=" + qty);

            int updated = productMapper.decreaseStock(productId, qty);
            log.info("PRODUCT 재고 UPDATE 결과: {}", updated);
            if (updated == 0) {
                throw new RefitException(ErrorCode.OUT_OF_STOCK,
                        "차감 실패(동시성): productId=" + productId + ", qty=" + qty);
            }
        }
    }

    private ConfirmPaymentResponse buildApprovedResponse(
            OrderRowDto order, ParsedPayment p, Long paymentId, List<OrderItemRowDto> orderItems) {

        var itemsForResponse = orderItems.stream().map(it -> ConfirmPaymentItemDto.builder()
                .productId(it.getProductId())
                .brandName(safe(it.getBrandName()))
                .productName(safe(it.getProductName()))
                .price(nz(it.getItemPrice()))
                .originalPrice(nzOr(it.getOrgUnitPrice(), nz(it.getItemPrice())))
                .quantity(it.getItemCount())
                .thumbnailUrl(safe(it.getThumbnailUrl()))
                .build()).toList();

        String firstThumb = itemsForResponse.isEmpty() ? null : itemsForResponse.get(0).getThumbnailUrl();
        int totalQty = itemsForResponse.stream().mapToInt(ConfirmPaymentItemDto::getQuantity).sum();

        return ConfirmPaymentResponse.builder()
                .paymentId(paymentId)
                .paymentKey(p.paymentKey)
                .totalAmount(p.totalAmount)
                .status("APPROVED")
                .receiptUrl(p.receiptUrl)
                .orderPk(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderName(order.getOrderSummary())
                .method(p.method)
                .firstItemThumb(firstThumb)
                .itemCount(totalQty)
                .items(itemsForResponse)
                .build();
    }



    @Override
    @Transactional
    public PartialCancelResponse partialCancel(Long orderItemId, PartialCancelRequest req, Long memberId) {
        // 0) 입력 검증
        validateCancelAmount(req);

        // 1) 아이템 조회/기초 검증
        OrderItemRowDto item = mustFindOrderItem(orderItemId);
        int cancelCount = validateAndGetCancelCount(item, req.getCancelAmount());

        // 2) 결제/주문 상태 조회
        PaymentRowDto pay = mustFindActivePayment(item.getOrderId());
        OrderRowDto moneyLocked = paymentMapper.findOrderMoneyForUpdate(item.getOrderId());
        int orderStatus = nz(moneyLocked.getOrderStatus()).intValue();

        boolean needsRefundAccount = needsRefundAccount(pay.getMethod(), orderStatus);
        log.debug("[cancel] method={}, orderStatus={}, needsRefundAccount={}", pay.getMethod(), orderStatus, needsRefundAccount);

        // 3) 멱등키
        final String idemp = (req.getIdempotencyKey()!=null && !req.getIdempotencyKey().isBlank())
                ? req.getIdempotencyKey() : UUID.randomUUID().toString();

        // 4) 배송비 정책 계산(잠금 하)
        ShippingAdjustment adj = computeShippingAdjustmentLocked(
                moneyLocked, pay.getPaymentId(), item.getOrderId(),
                orderItemId, cancelCount, req.getCancelAmount());

        long pgCancelAmount = adj.pgCancelAmount;
        if (pgCancelAmount > pay.getBalanceAmount()) {
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "환불 금액이 결제 잔액을 초과합니다.");
        }

        // 5) PG 바디 구성 + 환불계좌 보강(필요 시 DB에서 끌어오기)
        Map<String, Object> body = buildBaseCancelBody(req.getCancelReason(), pgCancelAmount, req.getTaxFreeAmount());
        PartialCancelRequest.RefundReceiveAccount rra = ensureRefundAccountIfNeeded(
                needsRefundAccount, req.getRefundReceiveAccount(), pay.getPaymentId(), item.getOrderId());

        if (needsRefundAccount) {
            // Toss는 필드명이 bankCode가 아니라 bank 입니다 (코드/숫자 모두 허용)
            putRefundAccountToBody(body, rra);
        }

        // 6) Toss 취소 호출 (필요액>0일 때만)
        var call = callTossCancelIfNeeded(pgCancelAmount, pay.getPaymentKey(), idemp, body);
        long newBalance = call.balanceAmount;
        Map<?,?> cancelObj = call.responseObj;

        // 7) 취소로그 + 수량 반영(조건부) + 상태 전이
        persistCancelLog(pay.getPaymentId(), idemp, pgCancelAmount, req.getTaxFreeAmount(),
                req.getCancelReason(), cancelObj, adj.shippingAdjApplied, needsRefundAccount ? rra : null);

        applyCanceledCountWithRaceTolerance(orderItemId, cancelCount, item);
        updatePaymentAndOrderStatusesIfNeeded(pay.getPaymentId(), item.getOrderId(), pgCancelAmount, newBalance);

        // 8) 푸시/응답
        notificationTriggerService.notifyPaymentCanceled(memberId, moneyLocked.getOrderId(),
                moneyLocked.getOrderSummary() + "의 결제가 취소되었습니다.");

        int pStatus = (newBalance == 0) ? 3 : 2; // 3=CANCELED, 2=PARTIAL_CANCELED
        return PartialCancelResponse.builder()
                .paymentId(pay.getPaymentId())
                .canceledAmount(pgCancelAmount)
                .balanceAmount(newBalance)
                .status((pStatus == 3) ? "CANCELED" : "PARTIAL_CANCELED")
                .canceledAt(LocalDateTime.now().toString())
                .build();
    }

    /* ========================= 보조 메서드들 ========================= */

    private void validateCancelAmount(PartialCancelRequest req) {
        Long reqAmt = req.getCancelAmount();
        if (reqAmt == null || reqAmt < 1) {
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 금액이 올바르지 않습니다.");
        }
    }

    private OrderItemRowDto mustFindOrderItem(Long orderItemId) {
        OrderItemRowDto item = paymentMapper.findOrderItemForCancel(orderItemId);
        if (item == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "주문 아이템을 찾을 수 없습니다.");
        return item;
    }

    private int validateAndGetCancelCount(OrderItemRowDto item, long cancelAmount) {
        int canceled = item.getCanceledCount() == null ? 0 : item.getCanceledCount();
        int remain   = item.getItemCount() - canceled;
        long unit    = item.getItemPrice();

        if (unit <= 0) throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "아이템 단가가 올바르지 않습니다.");
        if (cancelAmount % unit != 0) throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 금액은 단가의 배수여야 합니다.");
        int cancelCount = (int)(cancelAmount / unit);
        if (cancelCount < 1 || cancelCount > remain) {
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 가능 수량을 초과했습니다.");
        }
        return cancelCount;
    }

    private PaymentRowDto mustFindActivePayment(Long orderId) {
        PaymentRowDto pay = paymentMapper.findActivePaymentByOrderId(orderId);
        if (pay == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다.");
        return pay;
    }

    private boolean needsRefundAccount(String method, int orderStatus) {
        boolean isVA = "VIRTUAL_ACCOUNT".equalsIgnoreCase(method);
        boolean isTransfer = "TRANSFER".equalsIgnoreCase(method);
        boolean vaDepositCompleted = isVA && (orderStatus != 12);
        return isTransfer || vaDepositCompleted;
    }

    private Map<String, Object> buildBaseCancelBody(String reason, long pgCancelAmount, Long taxFreeAmount) {
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", reason);
        body.put("cancelAmount", pgCancelAmount);
        body.put("currency", "KRW");
        if (taxFreeAmount != null) body.put("taxFreeAmount", taxFreeAmount);
        return body;
    }

    // refundReceiveAccount를 요청/DB/과거로그에서 확보
    private PartialCancelRequest.RefundReceiveAccount ensureRefundAccountIfNeeded(
            boolean needsRefundAccount,
            PartialCancelRequest.RefundReceiveAccount fromReq,
            Long paymentId,
            Long orderId
    ) {
        if (!needsRefundAccount) return null;

        PartialCancelRequest.RefundReceiveAccount rra = fromReq;

        if (rra == null) {
            // 1순위: 이번 결제의 PAYMENT.VA_* (bankCode/accountNo/holderName)
            Map<String, Object> vaInfo = paymentMapper.findVaRefundInfoByPaymentId(paymentId);
            rra = buildRraIfValid(vaInfo);
        }
        if (rra == null) {
            // 2순위: 같은 주문에서 최신 VA 결제의 VA_* (백업)
            Map<String, Object> vaByOrder = paymentMapper.findLatestVaInfoByOrderId(orderId);
            rra = buildRraIfValid(vaByOrder);
        }
        if (rra == null) {
            // 3순위: 회원의 과거 환불계좌(PAYMENT_CANCEL 최근 기록)
            Long mid = paymentMapper.findMemberIdByOrderId(orderId);
            Map<String, Object> last = paymentMapper.findLastRefundAccountByMemberId(mid);
            rra = buildRraIfValid(last);
        }

        if (rra == null) {
            throw new RefitException(ErrorCode.INVALID_REQUEST_PARAMS,
                    "가상계좌/계좌이체 환불에는 refundReceiveAccount가 필요합니다.");
        }
        return rra;
    }

    private PartialCancelRequest.RefundReceiveAccount buildRraIfValid(Map<String, Object> src) {
        if (src == null) return null;
        String bank = mget(src, "bankCode");
        String acc  = mget(src, "accountNo");
        String name = mget(src, "holderName");
        if (isDashOrBlank(bank) || isDashOrBlank(acc) ) return null;
        return PartialCancelRequest.RefundReceiveAccount.builder()
                .bankCode(bank).accountNumber(acc).holderName(name).build();
    }

    private void putRefundAccountToBody(Map<String, Object> body, PartialCancelRequest.RefundReceiveAccount rra) {
        String bankCode = normalizeBankCode(rra.getBankCode());
        String accountNo = rra.getAccountNumber().replaceAll("[^0-9]", "");
        String holder    = rra.getHolderName().trim();
        if (isBlank(bankCode) || accountNo.length() < 6 || isBlank(holder)) {
            throw new RefitException(ErrorCode.INVALID_REQUEST_PARAMS, "환불 계좌정보 형식이 올바르지 않습니다.");
        }
        body.put("refundReceiveAccount", Map.of(
                "bank", bankCode,
                "accountNumber", accountNo,
                "holderName", holder
        ));
    }

    private static class CancelCallResult {
        final Map<?,?> responseObj;
        final long     balanceAmount;
        CancelCallResult(Map<?,?> r, long b) { this.responseObj = r; this.balanceAmount = b; }
    }

    private CancelCallResult callTossCancelIfNeeded(long pgCancelAmount, String paymentKey, String idemp, Map<String, Object> body) {
        if (pgCancelAmount <= 0) {
            return new CancelCallResult(Map.of("message", "shipping adjustment only, no PG refund"), 0L);
        }
        Map<?,?> cancelObj;
        try {
            cancelObj = tossWebClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header("Idempotency-Key", idemp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Toss cancel HTTP error: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "PG 결제 취소 호출 실패");
        }
        if (!(cancelObj.get("balanceAmount") instanceof Number n)) {
            throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "PG 응답 이상(잔액 누락)");
        }
        return new CancelCallResult(cancelObj, n.longValue());
    }

    private void persistCancelLog(Long paymentId, String idemp, long pgCancelAmount, Long taxFreeAmount,
            String reason, Map<?,?> cancelObj, int shippingAdjApplied,
            PartialCancelRequest.RefundReceiveAccount rraOrNull) {
        cancelLogService.logCancel(
                paymentId,
                idemp,
                pgCancelAmount,
                (taxFreeAmount==null?0L:taxFreeAmount),
                reason,
                LocalDateTime.now(),
                toJson(cancelObj),
                shippingAdjApplied,
                rraOrNull != null ? rraOrNull.getBankCode()    : null,
                rraOrNull != null ? rraOrNull.getAccountNumber(): null,
                rraOrNull != null ? rraOrNull.getHolderName()  : null
        );
    }

    private void applyCanceledCountWithRaceTolerance(Long orderItemId, int cancelCount, OrderItemRowDto before) {
        int updated = paymentMapper.conditionalIncreaseCanceledCount(orderItemId, cancelCount);
        if (updated <= 0) {
            boolean logged = paymentMapper.existsPaymentCancelByReqId(null); // 멱등키 확인은 호출부에서 처리했지만, 필요 시 확장
            OrderItemRowDto fresh = paymentMapper.findOrderItemForCancel(orderItemId);
            int remainBefore = before.getItemCount() - (before.getCanceledCount()==null?0:before.getCanceledCount());
            int remainAfter  = fresh.getItemCount()  - (fresh.getCanceledCount()==null?0:fresh.getCanceledCount());
            if (!logged && remainAfter >= remainBefore) {
                throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "이미 취소되었거나 취소 가능 수량을 초과했습니다.");
            }
            log.warn("No rows updated (race or already applied). orderItemId={}, beforeRemain={}, afterRemain={}",
                    orderItemId, remainBefore, remainAfter);
        }
    }

    private void updatePaymentAndOrderStatusesIfNeeded(Long paymentId, Long orderId, long pgCancelAmount, long newBalance) {
        if (pgCancelAmount > 0) {
            int pStatus = (newBalance == 0) ? 3 : 2; // 3=CANCELED, 2=PARTIAL_CANCELED
            paymentMapper.updatePaymentStatusAndBalance(paymentId, pStatus, newBalance);
        }
        Map<String, Object> agg = paymentMapper.aggregateOrderCancelState(orderId);
        int fullCanceled = ((Number)(agg.get("FULL_CANCELED")==null?0:agg.get("FULL_CANCELED"))).intValue();
        int total        = ((Number)(agg.get("TOTAL")==null?0:agg.get("TOTAL"))).intValue();
        if (total > 0) {
            if (fullCanceled == total) paymentMapper.updateOrderStatus(orderId, 3);
            else if (fullCanceled > 0) paymentMapper.updateOrderStatus(orderId, 2);
        }
    }

    private String toJson(Object src) {
        try { return om.writeValueAsString(src); } catch (Exception e) { return null; }
    }

    private static class ShippingAdjustment {
        long pgCancelAmount;     // PG로 보내는 실제 환불금
        int  shippingAdjApplied; // 1=배송비 조정(차감/환불) 했음
        long delta;              // (+)배송비 환불 / (-)배송비 차감
    }

    private ShippingAdjustment computeShippingAdjustmentLocked(
            OrderRowDto moneyLocked, Long paymentId, Long orderId,
            Long orderItemId, int cancelCount, long cancelAmount) {

        ShippingAdjustment r = new ShippingAdjustment();
        r.pgCancelAmount = cancelAmount;
        r.delta = 0;
        r.shippingAdjApplied = 0;

        long goodsAmount = nz(moneyLocked.getGoodsAmount());
        long deliveryFee = nz(moneyLocked.getDeliveryFee());
        int  orderStatus = moneyLocked.getOrderStatus()==null?0:moneyLocked.getOrderStatus();

        boolean hadAdjBefore = paymentMapper.existsShippingAdjApplied(paymentId); // 과거 -3,000 적용 여부(또는 어떤 조정이든 기록)

        // 1) 전액 환불 + 배송 전: 배송비는 최종적으로 0이 되어야 함
        long canceledSoFar = 0L;
        for (OrderItemRowDto it : paymentMapper.findOrderItems(orderId)) {
            int cc = it.getCanceledCount()==null?0:it.getCanceledCount();
            canceledSoFar += it.getItemPrice() * cc;
            if (Objects.equals(it.getOrderItemId(), orderItemId)) {
                canceledSoFar += it.getItemPrice() * cancelCount;
            }
        }
        long remainAfter = Math.max(0L, goodsAmount - canceledSoFar);

        if (remainAfter == 0 && orderStatus < 5) { // 전액 취소 + 배송 전
            if (deliveryFee > 0) {
                // 유료배송이었으면 유료배송비 전액 환불
                r.delta = deliveryFee;
                r.pgCancelAmount = cancelAmount + r.delta;
                r.shippingAdjApplied = 1;
                return r;
            } else if (hadAdjBefore) {
                // 무료배송 주문에서 과거에 -3,000 차감한 적이 있다면, 되돌려준다 (+3,000)
                r.delta = BASE_DELIVERY_FEE;
                r.pgCancelAmount = cancelAmount + r.delta;
                r.shippingAdjApplied = 1;
                return r;
            }
            // 무료배송이고 과거 차감도 없었다면 조정 없음
            return r;
        }

        // 2) 무료배송 → 임계 미만으로 처음 내려가는 순간 -3,000 차감 (아직 전액취소가 아님)
        boolean originallyFree = (deliveryFee == 0 && goodsAmount >= FREE_SHIPPING_THRESHOLD);
        if (originallyFree && remainAfter < FREE_SHIPPING_THRESHOLD && !hadAdjBefore) {
            r.delta = -BASE_DELIVERY_FEE;
            r.pgCancelAmount = Math.max(0L, cancelAmount + r.delta);
            r.shippingAdjApplied = 1;
            return r;
        }

        return r;
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static Long nz(Number n) { return n == null ? 0L : n.longValue(); }
    private static Long nzOr(Number n, Long fallback) { return n == null ? fallback : n.longValue(); }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static java.time.LocalDateTime parseTs(String iso) {
        if (iso == null || iso.isBlank()) return null;
        // 예: "2025-09-13T15:53:34+09:00"
        return java.time.OffsetDateTime.parse(iso).toLocalDateTime();
    }

    private String normalizeBankCode(String code) {
        if (code == null) return null;
        String c = code.trim();
        // 숫자 3자리면 그대로, 그 외는 대문자 영문코드로
        if (c.matches("^\\d{3}$")) return c;
        return c.toUpperCase();
    }

    private static String mget(Map<String, Object> m, String... keys) {
        if (m == null) return null;
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) v = m.get(k.toUpperCase());
            if (v == null) v = m.get(k.toLowerCase());
            if (v != null) return Objects.toString(v, null);
        }
        return null;
    }

    private static boolean isDashOrBlank(String s) {
        return s == null || s.trim().isEmpty() || "-".equals(s.trim());
    }

}
