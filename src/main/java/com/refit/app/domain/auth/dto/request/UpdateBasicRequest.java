package com.refit.app.domain.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBasicRequest {

    @Size(min = 1, max = 50)
    private String nickname;
    
    @Size(min = 1, max = 50)
    private String name;

    // 비밀번호는 선택 변경
    @Pattern(
            regexp = "^$|^(?=.{8,64}$)(?:(?=.*[A-Za-z])(?=.*\\d)|(?=.*[A-Za-z])(?=.*[^\\w\\s])|(?=.*\\d)(?=.*[^\\w\\s])).*$",
            message = "비밀번호 규칙 위반"
    )
    private String password;

    @Min(value = 0)
    @Max(value = 99999)
    private String zipcode;

    private String roadAddress;
    private String detailAddress;

    // yyyy-MM-dd
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$")
    private String birthday;

    @Pattern(regexp = "^$|^010\\d{8}$")
    private String phone;
}
