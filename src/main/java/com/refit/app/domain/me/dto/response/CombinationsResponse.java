package com.refit.app.domain.me.dto.response;

import com.refit.app.domain.me.dto.MyCombinationDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CombinationsResponse {
    private List<MyCombinationDto> combinations;
}
