package com.refit.app.auth.domain;

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
