package com.refit.app.domain.combination.mapper;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import com.refit.app.domain.combination.dto.CombinationResponseDto;
import com.refit.app.domain.combination.dto.response.MyCombinationResponse;
import com.refit.app.domain.me.dto.MyCombinationDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CombinationMapper {
    MyCombinationResponse findCombinationById(@Param("combinationId") Long combinationId);

    List<CombinationProductDto> findProductsByCombinationId(@Param("combinationId") Long combinationId);

    List<MyCombinationDto> findCombinationsByIds(@Param("ids") List<Long> ids);

    int increaseLike(@Param("combinationId") Long combinationId);
    int decreaseLike(@Param("combinationId") Long combinationId);

    List<CombinationResponseDto> findCombinations(@Param("bhType") Integer bhType,
            @Param("sort") String sort,
            @Param("combinationId") Long combinationId,
            @Param("limit") Integer limit);

    Long countCombinations();

    List<String> findProductImagesByCombinationId(@Param("combinationId") Long combinationId);
}
