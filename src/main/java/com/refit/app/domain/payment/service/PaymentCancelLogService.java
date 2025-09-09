package com.refit.app.domain.payment.service;

import java.time.LocalDateTime;

public interface PaymentCancelLogService {
    void logCancel(Long paymentId, String cancelRequestId, Long cancelAmount, Long taxFreeAmount,
            String cancelReason, LocalDateTime canceledAt, String rawJson,
            Integer shippingAdjApplied, String refundBankCode, String refundAccountNo,
            String refundHolderName);
}
