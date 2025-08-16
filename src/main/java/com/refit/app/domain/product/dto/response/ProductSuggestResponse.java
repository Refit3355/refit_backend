package com.refit.app.domain.product.dto.response;

import com.refit.app.domain.product.dto.ProductSimpleDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductSuggestResponse {
    private List<ProductSimpleDto> items;
    private boolean hasMore;
    private String nextCursor; // null 가능
}
