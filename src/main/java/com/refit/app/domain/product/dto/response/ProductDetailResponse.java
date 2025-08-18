package com.refit.app.domain.product.dto.response;

import com.refit.app.domain.product.dto.ImageDto;
import com.refit.app.domain.product.dto.ProductDetailDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductDetailResponse {
    private ProductDetailDto product;
    private List<ImageDto> images;
}
