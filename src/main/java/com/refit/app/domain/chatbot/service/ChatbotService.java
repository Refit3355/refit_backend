package com.refit.app.domain.chatbot.service;

import com.refit.app.domain.chatbot.dto.request.ProductSearchRequest;
import com.refit.app.domain.chatbot.dto.response.ProductSearchListResponse;

public interface ChatbotService {
    ProductSearchListResponse searchProducts(ProductSearchRequest request);
}