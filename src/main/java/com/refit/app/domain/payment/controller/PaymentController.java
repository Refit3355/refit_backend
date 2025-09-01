package com.refit.app.domain.payment.controller;

import com.refit.app.domain.payment.dto.request.ConfirmPaymentRequest;
import com.refit.app.domain.payment.dto.request.PartialCancelRequest;
import com.refit.app.domain.payment.dto.response.ConfirmPaymentResponse;
import com.refit.app.domain.payment.dto.response.PartialCancelResponse;
import com.refit.app.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    // 결제 승인(앱 success 딥링크 이후)
    @PostMapping("/confirm")
    public ConfirmPaymentResponse confirm(@RequestBody @Valid ConfirmPaymentRequest req) {
        return service.confirm(req);
    }

    // 부분취소
    @PostMapping("/{orderItemId}/cancel")
    public PartialCancelResponse cancelByItem(@PathVariable("orderItemId") long paymentId,
            @RequestBody @Valid PartialCancelRequest req) {
        return service.partialCancel(paymentId, req);
    }
}
