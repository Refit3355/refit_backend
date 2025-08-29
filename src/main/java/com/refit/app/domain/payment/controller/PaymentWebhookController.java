package com.refit.app.domain.payment.controller;

import com.refit.app.domain.payment.service.PaymentWebhookService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody Map<String,Object> payload) {
        // 토스는 eventType/createdAt/data 구조 (결제 상태 변경/가상계좌 콜백)
        webhookService.handle(payload);
        return ResponseEntity.ok("OK");
    }
}
