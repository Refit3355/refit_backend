package com.refit.app.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberRowDto {

    public Long memberId;
    public String email;
    public String nickname;
    public String memberName;
    public String password;
}
