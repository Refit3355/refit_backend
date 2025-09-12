package com.refit.app.domain.combination.mapper;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import com.refit.app.domain.combination.dto.CombinationResponseDto;
import com.refit.app.domain.combination.dto.response.CombinationDetailResponse;
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

    List<CombinationResponseDto> findCombinations(
            @Param("bhType") Integer bhType,
            @Param("sort") String sort,
            @Param("combinationId") Long combinationId,
            @Param("limit") Integer limit,
            @Param("keyword") String keyword,
            @Param("searchMode") String searchMode
    );

    Long countCombinations(
            @Param("bhType") Integer bhType,
            @Param("keyword") String keyword,
            @Param("searchMode") String searchMode
    );

    List<String> findProductImagesByCombinationId(@Param("combinationId") Long combinationId);

    CombinationDetailResponse findCombinationDetail(@Param("combinationId") Long combinationId);

    List<CombinationProductDto> findCombinationProducts(@Param("combinationId") Long combinationId);

    Long getNextCombinationId();

    void insertCombination(@Param("id") Long id,
            @Param("memberId") Long memberId,
            @Param("name") String name,
            @Param("content") String content,
            @Param("bhType") Integer bhType);

    void insertCombinationItem(@Param("combinationId") Long combinationId,
            @Param("productId") Long productId);
}
