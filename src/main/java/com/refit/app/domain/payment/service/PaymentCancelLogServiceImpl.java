package com.refit.app.domain.payment.service;

import com.refit.app.domain.payment.mapper.PaymentMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCancelLogServiceImpl implements PaymentCancelLogService {
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCancel(Long paymentId, String cancelRequestId, Long cancelAmount, Long taxFreeAmount,
            String cancelReason, LocalDateTime canceledAt, String rawJson,
            Integer shippingAdjApplied) {
        paymentMapper.insertPaymentCancel(paymentId, cancelRequestId, cancelAmount, taxFreeAmount,
                cancelReason, canceledAt, rawJson, shippingAdjApplied);
    }
}
