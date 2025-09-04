package com.refit.app.domain.payment.service;

import com.refit.app.domain.payment.dto.request.ConfirmPaymentRequest;
import com.refit.app.domain.payment.dto.request.PartialCancelRequest;
import com.refit.app.domain.payment.dto.response.ConfirmPaymentResponse;
import com.refit.app.domain.payment.dto.response.PartialCancelResponse;

public interface PaymentService {
    ConfirmPaymentResponse confirm(ConfirmPaymentRequest req, Long memberId);
    PartialCancelResponse partialCancel(Long paymentId, PartialCancelRequest req, Long memberId);
}
