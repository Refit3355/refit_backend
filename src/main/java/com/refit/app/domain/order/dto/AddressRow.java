package com.refit.app.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRow {
    private String receiverName;
    private String phone;
    private String roadAddress;
    private String detailAddress;
    private String zipcode;
    private String memo;
}
