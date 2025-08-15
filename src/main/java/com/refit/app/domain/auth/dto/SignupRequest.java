package com.refit.app.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@AllArgsConstructor
public class SignupRequest {

    @Email
    @NotBlank
    private String email;
    @Size(min = 1, max = 50)
    private String nickName;
    @Size(min = 1, max = 50)
    private String memberName;
    @NotBlank
    @Pattern(regexp = "^(?=.{8,64}$)(?:(?=.*[A-Za-z])(?=.*\\d)|(?=.*[A-Za-z])(?=.*[^\\w\\s])|(?=.*\\d)(?=.*[^\\w\\s])).*$")
    private String password;
    @NotNull
    private Integer zipcode;
    @NotBlank
    private String roadAddress;
    @NotBlank
    private String detailAddress;
    @Pattern(regexp = "male|female")
    @NotBlank
    private String gender;
    @NotNull
    @DateTimeFormat(pattern = "yyyy-mm-dd")
    private LocalDate birthday;
    private String profileUrl;
    @Pattern(regexp = "010\\d{8}")
    @NotBlank
    private String phoneNumber;


}
