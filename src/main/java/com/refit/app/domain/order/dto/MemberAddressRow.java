package com.refit.app.domain.order.dto;

import lombok.*;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAddressRow {
    private String memberName;
    private String phoneNumber;
    private String roadAddress;
    private String detailAddress;
    private long zipcode;
    private String deliveryNote;
}
