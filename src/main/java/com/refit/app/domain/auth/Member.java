package com.refit.app.domain.auth;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Member {

    private Long memberId;
    private String email;
    private String nickname;
    private String name;
    private String passwordHash;
    private Integer zipcode;
    private String roadAddress;
    private String detailAddress;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String profileImage;
}
