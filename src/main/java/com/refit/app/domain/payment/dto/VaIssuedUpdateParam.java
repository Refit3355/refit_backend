package com.refit.app.domain.payment.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaIssuedUpdateParam {
    private long paymentId;
    private Integer status;
    private String vaAccountNo;
    private String vaBankCode;
    private String vaAccountType;
    private String vaCustomerName;
    private String vaDepositorName;
    private LocalDateTime vaDueDate;
    private String vaSecret;
    private String rawJson;
}
