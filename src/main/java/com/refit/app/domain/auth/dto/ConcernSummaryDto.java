package com.refit.app.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConcernSummaryDto {

    private HealthInfoDto health;
    private HairInfoDto hair;
    private SkinInfoDto skin;
}
