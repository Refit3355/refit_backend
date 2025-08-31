package com.refit.app.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

//품목 단위 부분취소 시 수량 기준으로 요청할 때 사용
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartialCancelItem {

    @NotNull
    private Long orderItemId;

    @Min(1)
    private Integer cancelCount;
}
