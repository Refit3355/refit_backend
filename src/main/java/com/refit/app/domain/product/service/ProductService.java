package com.refit.app.domain.product.service;

import com.refit.app.domain.product.dto.response.ProductDetailResponse;
import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.model.SortType;

public interface ProductService {

    ProductListResponse getProducts(Integer categoryId, String group, SortType sortType, String cursor, int limit);

    ProductDetailResponse getProductDetail(Long id);
}
