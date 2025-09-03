package com.refit.app.domain.memberProduct.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExpiryCandidate {
    private Long memberProductId;
    private Long memberId;
    private Long productId;
    private String productName;
    private String brandName;
    private LocalDate expiryDate;
}
