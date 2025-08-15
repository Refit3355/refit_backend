package com.refit.app.domain.auth.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ConcernRequest {

    @Valid
    private HealthRequest health;
    @Valid
    private HairRequest hair;
    @Valid
    private SkinRequest skin;
}
