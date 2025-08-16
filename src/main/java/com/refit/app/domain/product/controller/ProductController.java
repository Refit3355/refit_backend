package com.refit.app.domain.product.controller;

import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.model.SortType;
import com.refit.app.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ProductListResponse getProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String group, // "beauty" | "health"
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        final SortType sortType = SortType.fromCode(sort);
        return productService.getProducts(categoryId, group, sortType, cursor, limit);
    }
}
