package com.refit.app.infra.cache;

/** 동일 조건 요청을 동일 키로 식별하기 위한 Redis 키 빌더 */
public final class RecommendationCacheKey {

    private RecommendationCacheKey() {}

    public static String build(
            int productType,
            Long memberId,
            String concernCode,
            String location,
            int topk,
            int limit
    ) {
        return String.format("reco:type%d:m%d:c%s:loc%s:tk%d:lim%d",
                productType,
                (memberId == null ? -1L : memberId),
                (concernCode == null || concernCode.isBlank() ? "none" : concernCode),
                (location == null || location.isBlank() ? "서울" : location),
                topk,
                limit
        );
    }
}
