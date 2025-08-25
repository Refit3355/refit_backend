package com.refit.app.domain.product.controller;

import com.refit.app.domain.product.dto.ProductDto;
import com.refit.app.domain.product.dto.request.LikedItemsRequest;
import com.refit.app.domain.product.dto.response.ProductDetailResponse;
import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.dto.response.ProductRecommendationResponse;
import com.refit.app.domain.product.dto.response.ProductSuggestResponse;
import com.refit.app.domain.product.model.SortType;
import com.refit.app.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductListResponse> getProducts(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "group", required = false) String group, // "beauty" | "health"
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        final SortType sortType = SortType.fromCode(sort);
        ProductListResponse response = productService.getProducts(categoryId, group, sortType, cursor, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable("id") Long id) {
        ProductDetailResponse response = productService.getProductDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ProductListResponse> searchProducts(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "cursor", required = false) String cursor
    ) {
        final SortType sortType = SortType.fromCode(sort);
        return ResponseEntity.ok(productService.searchProductsByName(q, sortType, limit, cursor));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ProductSuggestResponse> suggest(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "cursor", required = false) String cursor
    ) {
        return ResponseEntity.ok(productService.suggestProducts(q, limit, cursor));
    }

    @PostMapping("/like")
    public ResponseEntity<ProductListResponse> getLikedProducts(@RequestBody LikedItemsRequest req) {
        if (req == null || req.getLikedItems() == null || req.getLikedItems().isEmpty()) {
            return ResponseEntity.ok(new ProductListResponse(List.of(), 0, false, null));
        }
        return ResponseEntity.ok(productService.getLikedProducts(req.getLikedItems()));
    }

    /**
     * GET /products/recommendation/{type}?limit={limit}
     * type: 0=전체, 1=뷰티, 2=헤어, 3=건강기능식품
     */
    @GetMapping("/recommendation/{type}")
    public ResponseEntity<ProductRecommendationResponse> getRecommendations(
            @PathVariable("type") int type,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal Long memberId
    ) {
        ProductRecommendationResponse resp = productService.getRecommendations(type, limit, memberId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<ProductDto>> getPopularProducts(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ){
        List<ProductDto> response = productService.findTopProductsByOrderCount(limit);
        return ResponseEntity.ok(response);
    }
}
