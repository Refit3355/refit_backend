package com.refit.app.domain.product.dto.response;

import com.refit.app.domain.product.dto.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductListResponse {
    private List<ProductDto> items;
    private int totalCount;
    private boolean hasMore;
    private String nextCursor;
}
