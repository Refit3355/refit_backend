package com.refit.app.infra.cache;

/** 동일 조건 요청을 동일 키로 식별하기 위한 Redis 키 빌더 */
public final class RecommendationCacheKey {

    private RecommendationCacheKey() {}

    public static String build(
            int productType, int limit,
            Long memberId, String location,
            Integer topk, Integer finalCount) {
        return String.format("reco:type%d:l%d:m%d:loc:%s:tk%s:f%s",
                productType,
                limit,
                (memberId == null ? -1L : memberId),
                (location == null ? "서울" : location),
                (topk == null ? "200" : topk),
                (finalCount == null ? String.valueOf(limit) : finalCount)
        );
    }
}
