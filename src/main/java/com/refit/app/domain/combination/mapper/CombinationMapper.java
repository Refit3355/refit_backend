package com.refit.app.domain.combination.mapper;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import com.refit.app.domain.combination.dto.response.CombinationResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CombinationMapper {
    CombinationResponse findCombinationById(Long combinationId);

    List<CombinationProductDto> findProductsByCombinationId(Long combinationId);
}
