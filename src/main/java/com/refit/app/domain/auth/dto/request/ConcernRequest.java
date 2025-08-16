package com.refit.app.domain.auth.dto.request;

import com.refit.app.domain.auth.dto.HairInfoDto;
import com.refit.app.domain.auth.dto.HealthInfoDto;
import com.refit.app.domain.auth.dto.SkinInfoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcernRequest {

    @Valid
    @NotNull
    private HealthInfoDto health;

    @Valid
    @NotNull
    private HairInfoDto hair;

    @Valid
    @NotNull
    private SkinInfoDto skin;
}
