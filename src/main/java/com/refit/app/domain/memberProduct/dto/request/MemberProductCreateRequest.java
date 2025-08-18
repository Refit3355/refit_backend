package com.refit.app.domain.memberProduct.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberProductCreateRequest {
    private String type; // "health" | "beauty"
    private String productName;
    private String brandName;
    private Integer recommendedPeriodDays;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate; // YYYY-MM-DD

    private Long categoryId;
    private List<Long> effect;
}
