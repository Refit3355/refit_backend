package com.refit.app.domain.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupAllRequest {

    @Valid
    @NotNull
    private SignupRequest signup;
    @Valid
    @NotNull
    private ConcernRequest concerns;

}
