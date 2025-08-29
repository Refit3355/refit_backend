package com.refit.app.domain.payment.service;

import java.util.Map;

public interface PaymentWebhookService {

    void handle(Map<String, Object> payload);
}
