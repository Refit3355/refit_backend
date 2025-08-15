package com.refit.app.domain.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcernRequest {

    @Valid
    @NotNull
    private HealthRequest health;

    @Valid
    @NotNull
    private HairRequest hair;

    @Valid
    @NotNull
    private SkinRequest skin;
}
