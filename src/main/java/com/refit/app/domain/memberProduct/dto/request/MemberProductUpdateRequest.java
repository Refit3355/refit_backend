package com.refit.app.domain.memberProduct.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberProductUpdateRequest {
    private String productName;
    private String brandName;
    private Long categoryId;
    private List<Long> effectIds;
    private Integer recommendedPeriod;
    private String startDate;
}
