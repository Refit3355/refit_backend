package com.refit.app.domain.product.controller;

import com.refit.app.domain.product.dto.response.ProductDetailResponse;
import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.dto.response.ProductSuggestResponse;
import com.refit.app.domain.product.model.SortType;
import com.refit.app.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<ProductListResponse> getProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String group, // "beauty" | "health"
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        final SortType sortType = SortType.fromCode(sort);
        ProductListResponse response = productService.getProducts(categoryId, group, sortType, cursor, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        ProductDetailResponse response = productService.getProductDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ProductListResponse> searchProducts(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        final SortType sortType = SortType.fromCode(sort);
        return ResponseEntity.ok(productService.searchProductsByName(q, sortType, limit, cursor));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ProductSuggestResponse> suggest(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(productService.suggestProducts(q, limit, cursor));
    }


}
