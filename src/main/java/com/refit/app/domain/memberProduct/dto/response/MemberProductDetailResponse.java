package com.refit.app.domain.memberProduct.dto.response;

import com.refit.app.domain.memberProduct.model.UsageStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberProductDetailResponse {
    private Long memberProductId;
    private String brandName;
    private String productName;
    private String categoryName;   // null 가능
    private Long productId;        // null 가능
    private UsageStatus status;        // USING(1) / COMPLETED(2)
    private Integer recommendedPeriod;
    private String startDate;      // 'YYYY-MM-DD'
    private String expiryDate;
    private Integer daysRemaining; // D-남은일수 (지난 경우 음수/표현은 displayRemaining)
    private String displayRemaining;  // 'D-30', 'D-day', 'D+2'
    private String usagePeriodText;   // COMPLETED일 때 "2025-01-01 ~ 2025-10-10 (총 XX일)"
    List<String> effects;
}
