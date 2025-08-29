package com.refit.app.domain.order.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingInfo {

    @NotBlank private String receiverName;
    @NotBlank private String phone;
    @NotBlank private String roadAddress;
    @NotBlank private String detailAddress;
    @NotBlank private Long zipcode;

    private String memo;
}
