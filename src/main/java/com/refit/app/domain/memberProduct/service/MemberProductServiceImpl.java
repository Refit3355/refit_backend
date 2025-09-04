package com.refit.app.domain.memberProduct.service;

import static com.refit.app.global.util.CursorUtil.asInt;

import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.dto.request.MemberProductUpdateRequest;
import com.refit.app.domain.memberProduct.dto.response.MemberProductDetailResponse;
import com.refit.app.domain.memberProduct.dto.response.ProductRecommendationDto;
import com.refit.app.domain.memberProduct.mapper.MemberProductMapper;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.memberProduct.model.UsageStatus;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberProductServiceImpl implements MemberProductService {

    private final MemberProductMapper memberProductMapper;

    // 하이퍼파라미터(운영 튜닝 대상)
    private static final double COMPAT_GOOD = 1.20;
    private static final double COMPAT_NEUTRAL = 1.00;
    private static final double CONCERN_BOOST_PER_EFFECT = 0.08;
    private static final double SAME_CATEGORY_BONUS = 1.10;
    private static final double SAME_GROUP_BONUS = 1.05;

    // 유사도 우선성을 크게 해치지 않는 소폭 지터(±3%)
    private static final double JITTER_SCALE = 0.06; // 0.06 => factor in [0.97, 1.03]

    // ===== 효과 ID 상수 =====
    // --- 피부(뷰티) ---
    private static final long EFFECT_MOISTURE       = 0L; // 보습
    private static final long EFFECT_SOOTHING       = 1L; // 진정
    private static final long EFFECT_ANTI_WRINKLE   = 2L; // 주름 개선
    private static final long EFFECT_WHITENING      = 3L; // 미백
    private static final long EFFECT_SUNBLOCK       = 4L; // 자외선 차단
    private static final long EFFECT_ACNE_RELIEF    = 5L; // 여드름 완화
    private static final long EFFECT_ITCH_RELIEF    = 6L; // 가려움 개선
    private static final long EFFECT_STRETCH_MARK   = 7L; // 튼살 개선

    // --- 헤어 ---
    private static final long EFFECT_DAMAGED_HAIR   = 8L; // 손상모 개선
    private static final long EFFECT_HAIR_LOSS      = 9L; // 탈모 개선
    private static final long EFFECT_SCALP_CARE     = 10L; // 두피 개선

    // --- 헬스 ---
    private static final long EFFECT_BLOOD_FLOW     = 11L; // 혈행 개선
    private static final long EFFECT_GUT_HEALTH     = 12L; // 장 건강
    private static final long EFFECT_IMMUNITY       = 13L; // 면역력 증진
    private static final long EFFECT_ANTIOX         = 14L; // 항산화
    private static final long EFFECT_EYE_HEALTH     = 15L; // 눈 건강
    private static final long EFFECT_BONE_HEALTH    = 16L; // 뼈 건강
    private static final long EFFECT_VITALITY       = 17L; // 활력
    private static final long EFFECT_SKIN_HEALTH    = 18L; // 피부 건강 (헬스 카테고리)

    @Override
    @Transactional
    public void createFromProduct(Long memberId, Long productId) {
        // 1. 상품 기본 정보 조회
        ProductSimpleRow product = memberProductMapper.findProductSimple(productId);
        if (product == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "상품이 존재하지 않습니다. id=" + productId);
        }

        // 2. INSERT 파라미터 구성
        LocalDate startDate = LocalDate.now();
        Integer recommendedExpirationDate = product.getRecommendedPeriod();

        // 3. INSERT
        memberProductMapper.insertMemberProduct(
                memberId,
                productId,
                startDate,
                recommendedExpirationDate,
                1, // USAGE_STATUS=1 (사용중)
                product.getProductName(),
                product.getBrandName(),
                product.getBhType(),
                product.getCategoryId()
        );
    }

    @Override
    @Transactional
    public void createCustom(Long memberId, MemberProductCreateRequest req) {
        Integer bhType = toBhType(req.getType());
        LocalDate startDate = req.getStartDate();
        Integer recommendedDays = req.getRecommendedPeriodDays();
        List<Long> effectIds = (req.getEffect() == null) ? Collections.emptyList() : req.getEffect();

        Long memberProductId = memberProductMapper.insertMemberProductWithEffects(
                memberId,
                null,
                startDate,
                recommendedDays,
                1,
                req.getProductName(),
                req.getBrandName(),
                bhType,
                req.getCategoryId(),
                effectIds
        );
        // memberProductId 사용 계획이 없으면 무시 가능
    }

    private int toBhType(ProductType type) {
        if (type == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "부적절한 인수 값 type: null");
        }
        return type.getCode();
    }

    private Integer toStatusCodeNullable(UsageStatus statusOrNull) {
        return (statusOrNull == null) ? null : statusOrNull.getCode();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberProductDetailResponse> getMemberProducts(Long memberId,
            ProductType type,
            UsageStatus status) {
        final int bhType = toBhType(type);
        final Integer statusCode = toStatusCodeNullable(status);
        return memberProductMapper.selectMemberProducts(memberId, bhType, statusCode);
    }

    @Override
    @Transactional
    public void deleteMemberProduct(Long memberId, Long memberProductId) {
        int updated = memberProductMapper.softDeleteMemberProduct(memberId, memberProductId);
        if (updated == 0) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "삭제 대상이 없거나 권한이 없습니다.");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long memberId, Long memberProductId, UsageStatus status) {
        if (status == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "status 는 필수입니다.");
        }
        int updated;
        switch (status) {
            case USING -> updated = memberProductMapper.markUsing(memberId, memberProductId);
            case COMPLETED -> updated = memberProductMapper.markCompleted(memberId, memberProductId);
            case DELETED -> throw new RefitException(
                    ErrorCode.ILLEGAL_ARGUMENT,
                    "삭제는 DELETE API를 사용하세요. status 변경 API는 using/completed만 허용합니다."
            );
            default -> throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "지원하지 않는 상태: " + status);
        }
        if (updated == 0) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "대상이 없거나 상태 변경 불가합니다.");
        }
    }

    @Override
    @Transactional
    public void updateMemberProduct(Long memberId, Long memberProductId, MemberProductUpdateRequest req) {

        // 현재 행의 소유/상태/타입 확인
        var row = memberProductMapper.findMemberProductMeta(memberId, memberProductId);
        if (row == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "대상이 없거나 권한이 없습니다.");
        if (row.getUsageStatus() != UsageStatus.USING.getCode()) {
            throw new RefitException(ErrorCode.STATUS_CONFLICT, "사용중(USING) 상태에서만 수정 가능합니다.");
        }

        if (row.getProductId() != null) {
            // in-app (productId 있음) → 기간/시작일만 수정
            memberProductMapper.updateInAppMemberProduct(
                    memberId, memberProductId, req.getRecommendedPeriod(), req.getStartDate()
            );
        } else {
            // external (productId 없음) → 이름/브랜드/효과/기간/시작일 수정
            memberProductMapper.updateExternalMemberProduct(
                    memberId, memberProductId,
                    req.getProductName(), req.getBrandName(),
                    req.getRecommendedPeriod(), req.getStartDate(),
                    req.getCategoryId()
            );

            // 효과 업데이트 (전체 교체)
            if (req.getEffectIds() != null) {
                List<Long> deduped = dedupEffectIds(req.getEffectIds()); // distinct
                memberProductMapper.deleteAllEffects(memberProductId);
                if (!deduped.isEmpty()) {
                    for (Long eid : deduped) {
                        memberProductMapper.insertEffect(memberId, memberProductId, eid);
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void createFromOrderItem(Long memberId, Long orderItemId) {
        final int inc = 1;

        // 대상 행 잠금
        Map<String, Object> row = memberProductMapper.lockOrderItemForUpdate(memberId, orderItemId);
        if (row == null || row.isEmpty()) {
            throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "대상 주문항목을 찾을 수 없거나 구매확정 상태가 아닙니다.");
        }

        Number remaining = (Number) row.get("REMAININGCOUNT");
        if (remaining == null) {
            remaining = (Number) row.get("remainingCount"); // 드라이버/맵핑에 따라 소문자로 올 수 있어 보조
        }
        long remainingCount = remaining != null ? remaining.longValue() : 0L;

        if (remainingCount < inc) {
            throw new RefitException(ErrorCode.STATUS_CONFLICT, "등록 가능한 수량이 없습니다.");
        }

        // MEMBER_PRODUCT 생성
        int inserted = memberProductMapper.insertMemberProductFromOrderItem(memberId, orderItemId);
        if (inserted == 0) {
            throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "사용등록 생성 중 오류가 발생했습니다.");
        }

        // USED_COUNT 증가
        int updated = memberProductMapper.increaseUsedCount(memberId, orderItemId, inc);
        if (updated == 0) {
            throw new RefitException(ErrorCode.STATUS_CONFLICT, "수량 업데이트에 실패했습니다.");
        }
    }

    // 입력 effectIds 정규화: null 제거 + 중복 제거(순서 보존)
    private List<Long> dedupEffectIds(List<Long> effectIds) {
        return effectIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRecommendationDto> recommendForMember(Long memberId, Long memberProductId, int topKPerBase, int finalLimit) {

        // 0) 소유 검증
        Long owner = memberProductMapper.selectOwnerMemberId(memberProductId);
        if (owner == null || !owner.equals(memberId)) {
            throw new RefitException(ErrorCode.ACCESS_DENIED, "해당 사용자의 memberProduct가 아닙니다");
        }

        // 1) 기준 memberProduct
        Map<String,Object> base = memberProductMapper.selectMemberProductBase(memberProductId);
        if (base == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "해당 사용중인 제품을 찾을 수 없습니다");

        Long baseProductId = getLong(base, "PRODUCT_ID");
        Integer baseCategoryId = getInt(base, "CATEGORY_ID");
        if (baseProductId != null && baseCategoryId == null) {
            Integer cat = asIntObj(memberProductMapper.selectProductCategory(baseProductId));
            if (cat == null) return List.of();
            baseCategoryId = cat;
        }

        // 2) 사용자 프로필
        Integer skinType = orDefault(asIntObj(memberProductMapper.selectMemberSkinType(memberId)), 1);
        Map<String,Object> skin   = orDefault(memberProductMapper.selectSkinConcerns(memberId), Map.of());
        Map<String,Object> hair   = orDefault(memberProductMapper.selectHairConcerns(memberId), Map.of());
        Map<String,Object> health = orDefault(memberProductMapper.selectHealthConcerns(memberId), Map.of());

        // 3) 고민 → 효과ID
        Set<Long> skinEffectIds   = wantedEffectsForSkin(skin);
        Set<Long> hairEffectIds   = wantedEffectsForHair(hair);
        Set<Long> healthEffectIds = wantedEffectsForHealth(health);

        if (baseProductId == null) {
            // 외부 제품 추천
            return recommendForExternal(memberId, memberProductId, baseCategoryId,
                    skinType, skinEffectIds, hairEffectIds, healthEffectIds, finalLimit);
        }

        // 내부 제품 추천
        return recommendForInternal(memberId, memberProductId, baseProductId, baseCategoryId,
                skinType, skinEffectIds, hairEffectIds, healthEffectIds, topKPerBase, finalLimit);
    }

    private List<ProductRecommendationDto> recommendForInternal(
            Long memberId, Long memberProductId, Long baseProductId, Integer baseCategoryId,
            Integer skinType, Set<Long> skinEffectIds, Set<Long> hairEffectIds, Set<Long> healthEffectIds,
            int topKPerBase, int finalLimit
    ) {
        // 사전계산 이웃(전카테고리)
        List<Map<String,Object>> neigh = memberProductMapper.selectNeighbors(baseProductId, topKPerBase);
        if (neigh.isEmpty()) return List.of();

        // 후보 ID
        List<Long> candidateIds = neigh.stream()
                .map(r -> getLong(r, "NEIGHBOR_PRODUCT_ID"))
                .filter(Objects::nonNull)
                .toList();

        // 후보 상세
        List<Map<String,Object>> prodRows = memberProductMapper.selectProductsByIds(candidateIds);
        Map<Long, Map<String,Object>> prodMap = prodRows.stream()
                .collect(Collectors.toMap(r -> getLong(r, "PRODUCT_ID"), r -> r));

        // 후보 효과
        List<Map<String,Object>> effRows = memberProductMapper.selectProductEffects(candidateIds);
        Map<Long, Set<Long>> productEffects = groupEffects(effRows);

        // 스킨 compat
        Map<Long, Integer> compatMap = loadCompatForCandidates(candidateIds, skinType);

        // 점수 계산
        Map<Long, Double> baseScores = new HashMap<>();
        Map<Long, Double> bestSim = new HashMap<>();
        Map<Long, Integer> bestRank = new HashMap<>();

        long seed = Objects.hash(memberId, memberProductId, LocalDate.now());
        for (Map<String,Object> n : neigh) {
            Long candId = getLong(n, "NEIGHBOR_PRODUCT_ID");
            if (candId == null || candId.equals(baseProductId)) continue;

            Map<String,Object> pd = prodMap.get(candId);
            if (pd == null) continue;

            long stock = getLong(pd, "STOCK");
            if (stock <= 0) continue;

            int candCat = getInt(pd, "CATEGORY_ID");
            double sim = getDouble(n, "SIM_OVERALL");
            int rank = getInt(n, "RANK_ORDER");

            // compat
            double compatWeight = 1.0;
            if (isSkin(candCat)) {
                Integer cp = compatMap.getOrDefault(candId, 0);
                if (cp == -1) continue;
                compatWeight = (cp == 1) ? COMPAT_GOOD : COMPAT_NEUTRAL;
            }

            // 고민-효과 매칭
            Set<Long> eids = productEffects.getOrDefault(candId, Set.of());
            long matched = concernMatchCount(candCat, eids, skinEffectIds, hairEffectIds, healthEffectIds);
            double concernWeight = 1.0 + CONCERN_BOOST_PER_EFFECT * matched;

            // 카테고리 보너스
            double catBonus = 1.0;
            if (baseCategoryId != null) {
                if (candCat == baseCategoryId) catBonus = SAME_CATEGORY_BONUS;
                else if (sameGroup(baseCategoryId, candCat)) catBonus = SAME_GROUP_BONUS;
            }

            double baseScore = sim * compatWeight * concernWeight * catBonus;
            baseScores.put(candId, baseScore);
            bestSim.merge(candId, sim, Math::max);
            bestRank.merge(candId, rank, Math::min);
        }

        // 30% / 70% 믹스
        List<Long> finalIds = mix30_70(baseScores, finalLimit);

        // DTO
        List<ProductRecommendationDto> out = mapToDto(
                finalIds, prodMap, baseScores, bestSim, bestRank, productEffects
        );

        // 브랜드 편중 제한
        out = limitPerBrand(out, 2);
        if (out.size() > finalLimit) out = out.subList(0, finalLimit);
        return out;
    }

    private List<ProductRecommendationDto> recommendForExternal(
            Long memberId, Long memberProductId, Integer baseCategoryId,
            Integer skinType, Set<Long> skinEffectIds, Set<Long> hairEffectIds, Set<Long> healthEffectIds,
            int finalLimit
    ) {
        List<Map<String,Object>> prodRows = memberProductMapper.selectAllProducts();
        Map<Long, Map<String,Object>> prodMap = prodRows.stream()
                .collect(Collectors.toMap(r -> getLong(r, "PRODUCT_ID"), r -> r));

        List<Map<String,Object>> effRows = memberProductMapper.selectAllProductEffects();
        Map<Long, Set<Long>> productEffects = groupEffects(effRows);

        // 외부 기준 효과 = 현재는 사용자 고민 효과 세트(카테고리 알면 해당 그룹만)
        Set<Long> extEffects = extDesiredEffectsFromConcerns(baseCategoryId, skinEffectIds, hairEffectIds, healthEffectIds);

        List<Long> allIds = prodRows.stream().map(r -> getLong(r,"PRODUCT_ID")).toList();
        Map<Long, Integer> compatMap = loadCompatForCandidates(allIds, skinType);

        Map<Long, Double> baseScores = new HashMap<>();
        Map<Long, Double> bestSim = new HashMap<>();
        Map<Long, Integer> bestRank = new HashMap<>();

        for (Map.Entry<Long, Map<String,Object>> e : prodMap.entrySet()) {
            Long pid = e.getKey();
            Map<String,Object> pd = e.getValue();

            long stock = getLong(pd, "STOCK");
            if (stock <= 0) continue;

            int candCat = getInt(pd, "CATEGORY_ID");
            Set<Long> candEffects = productEffects.getOrDefault(pid, Set.of());

            double sim = cosineOnSets(extEffects, candEffects);
            if (sim <= 0) continue;

            double compatWeight = 1.0;
            if (isSkin(candCat)) {
                Integer cp = compatMap.getOrDefault(pid, 0);
                if (cp == -1) continue;
                compatWeight = (cp == 1) ? COMPAT_GOOD : COMPAT_NEUTRAL;
            }

            long matched = concernMatchCount(candCat, candEffects, skinEffectIds, hairEffectIds, healthEffectIds);
            double concernWeight = 1.0 + CONCERN_BOOST_PER_EFFECT * matched;

            double catBonus = 1.0;
            if (baseCategoryId != null) {
                if (candCat == baseCategoryId) catBonus = SAME_CATEGORY_BONUS;
                else if (sameGroup(baseCategoryId, candCat)) catBonus = SAME_GROUP_BONUS;
            }

            double baseScore = sim * compatWeight * concernWeight * catBonus;
            baseScores.put(pid, baseScore);
            bestSim.merge(pid, sim, Math::max);
            bestRank.merge(pid, 9999, Math::min); // 외부는 rank 없음
        }

        List<Long> finalIds = mix30_70(baseScores, finalLimit);
        List<ProductRecommendationDto> out = mapToDto(
                finalIds, prodMap, baseScores, bestSim, bestRank, productEffects
        );

        out = limitPerBrand(out, 2);
        if (out.size() > finalLimit) out = out.subList(0, finalLimit);
        return out;
    }

    // ===== 유틸 =====
    private Map<Long, Set<Long>> groupEffects(List<Map<String,Object>> effRows) {
        Map<Long, Set<Long>> res = new HashMap<>();
        for (Map<String,Object> r : effRows) {
            Long pid = getLong(r, "PRODUCT_ID");
            Long eid = getLong(r, "EFFECT_ID");
            if (pid == null || eid == null) continue;
            res.computeIfAbsent(pid, k -> new HashSet<>()).add(eid);
        }
        return res;
    }

    private Map<Long, Integer> loadCompatForCandidates(List<Long> candidateIds, Integer skinType) {
        Map<Long, Integer> compatMap = new HashMap<>();
        if (candidateIds == null || candidateIds.isEmpty() || skinType == null) return compatMap;
        for (Map<String,Object> row : memberProductMapper.selectSkinCompat(candidateIds, skinType)) {
            compatMap.put(getLong(row, "PRODUCT_ID"), getInt(row, "COMPAT"));
        }
        return compatMap;
    }

    private List<Long> mix30_70(Map<Long, Double> baseScores, int finalLimit) {
        if (baseScores.isEmpty()) return List.of();
        List<Map.Entry<Long, Double>> allByBase = baseScores.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        int totalNeed = finalLimit;
        int coreCount = Math.max(1, (int)Math.floor(totalNeed * 0.30));
        int varCount  = Math.max(0, totalNeed - coreCount);

        List<Long> coreIds = allByBase.stream().limit(coreCount).map(Map.Entry::getKey).toList();

        Set<Long> coreSet = new HashSet<>(coreIds);
        List<Map.Entry<Long, Double>> varPool = allByBase.stream()
                .filter(e -> !coreSet.contains(e.getKey()))
                .toList();

        List<Long> variableIds = weightedSampleWithoutReplacement(varPool, varCount);

        List<Long> finalIds = new ArrayList<>(coreIds.size()+variableIds.size());
        finalIds.addAll(coreIds);
        finalIds.addAll(variableIds);
        return finalIds;
    }

    private List<ProductRecommendationDto> mapToDto(
            List<Long> ids,
            Map<Long, Map<String,Object>> prodMap,
            Map<Long, Double> baseScores,
            Map<Long, Double> bestSim,
            Map<Long, Integer> bestRank,
            Map<Long, Set<Long>> productEffects
    ) {
        List<ProductRecommendationDto> out = new ArrayList<>();
        for (Long pid : ids) {
            Map<String,Object> pd = prodMap.get(pid);
            if (pd == null) continue;

            out.add(ProductRecommendationDto.builder()
                    .productId(pid)
                    .categoryId(getInt(pd, "CATEGORY_ID"))
                    .productName((String) pd.get("PRODUCT_NAME"))
                    .brandName((String) pd.get("BRAND_NAME"))
                    .price(getLong(pd, "PRICE"))
                    .stock(getLong(pd, "STOCK"))
                    .thumbnailUrl((String) pd.get("THUMBNAIL_URL"))
                    .discountRate(getInt(pd, "DISCOUNT_RATE"))
                    .discountedPrice(getLong(pd, "DISCOUNTED_PRICE"))
                    .score(baseScores.getOrDefault(pid, 0.0))
                    .baseSimilarity(bestSim.getOrDefault(pid, 0.0))
                    .rankOrder(bestRank.getOrDefault(pid, 9999))
                    .effectIds(new ArrayList<>(productEffects.getOrDefault(pid, Set.of())))
                    .build());
        }
        return out;
    }

    private long concernMatchCount(
            int candCat, Set<Long> candEffects,
            Set<Long> skinEffectIds, Set<Long> hairEffectIds, Set<Long> healthEffectIds
    ) {
        if (isSkin(candCat))      return countMatches(candEffects, skinEffectIds);
        else if (isHair(candCat)) return countMatches(candEffects, hairEffectIds);
        else if (isHealth(candCat)) return countMatches(candEffects, healthEffectIds);
        return 0;
    }

    private Set<Long> extDesiredEffectsFromConcerns(Integer baseCategoryId,
            Set<Long> skinEffectIds, Set<Long> hairEffectIds, Set<Long> healthEffectIds) {
        if (baseCategoryId == null) {
            Set<Long> u = new HashSet<>();
            u.addAll(skinEffectIds);
            u.addAll(hairEffectIds);
            u.addAll(healthEffectIds);
            return u;
        }
        if (isSkin(baseCategoryId))   return skinEffectIds;
        if (isHair(baseCategoryId))   return hairEffectIds;
        if (isHealth(baseCategoryId)) return healthEffectIds;
        return Set.of();
    }

    // ==== 효과 매핑 ====
    private Set<Long> wantedEffectsForSkin(Map<String,Object> skin) {
        Set<Long> s = new HashSet<>();
        if (on(skin,"ATOPIC"))           s.add(6L);       // 가려움 개선
        if (on(skin,"ACNE"))             { s.add(5L); s.add(1L); } // 여드름 완화, 진정
        if (on(skin,"WHITENING"))        s.add(3L);       // 미백
        if (on(skin,"SEBUM"))            s.add(5L);
        if (on(skin,"INNER_DRYNESS"))    s.add(0L);       // 보습
        if (on(skin,"WRINKLES"))         s.add(2L);       // 주름 개선
        if (on(skin,"ENLARGED_PORES"))   s.add(5L);
        if (on(skin,"REDNESS"))          s.add(1L);
        if (on(skin,"KERATIN"))          s.add(1L);
        return s;
    }
    private Set<Long> wantedEffectsForHair(Map<String,Object> hair) {
        Set<Long> s = new HashSet<>();
        if (on(hair,"HAIR_LOSS"))        s.add(9L);  // 탈모 개선
        if (on(hair,"DAMAGED_HAIR"))     s.add(8L);  // 손상모 개선
        if (on(hair,"SCALP_TROUBLE"))    s.add(8L);  // 두피 개선
        if (on(hair,"DANDRUFF"))         s.add(8L);
        return s;
    }

    private Set<Long> wantedEffectsForHealth(Map<String,Object> health) {
        Set<Long> s = new HashSet<>();
        if (on(health,"EYE_HEALTH"))        s.add(EFFECT_EYE_HEALTH);
        if (on(health,"FATIGUE"))           s.add(EFFECT_VITALITY);
        if (on(health,"SLEEP_STRESS"))      s.add(EFFECT_ANTIOX);
        if (on(health,"IMMUNE_CARE"))       s.add(EFFECT_IMMUNITY);
        if (on(health,"MUSCLE_HEALTH"))     s.add(EFFECT_BONE_HEALTH);
        if (on(health,"GUT_HEALTH"))        s.add(EFFECT_GUT_HEALTH);
        if (on(health,"BLOOD_CIRCULATION")) s.add(EFFECT_BLOOD_FLOW);
        return positiveOnly(s);
    }

    private boolean on(Map<String,Object> m, String k) { return m != null && asInt(m.get(k)) == 1; }

    private Set<Long> positiveOnly(Set<Long> s) {
        return s.stream().filter(id -> id != null && id >= 0).collect(Collectors.toSet());
    }

    // ==== 수학/도우미 ====
    private double cosineOnSets(Set<Long> a, Set<Long> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;
        int inter = 0;
        for (Long x : a) if (b.contains(x)) inter++;
        if (inter == 0) return 0.0;
        double denom = Math.sqrt((double)a.size() * (double)b.size());
        if (denom <= 0) return 0.0;
        return inter / denom;
    }

    private boolean isSkin(int c)   { return c >= 0 && c <= 5; }
    private boolean isHair(int c)   { return c == 6 || c == 7; }
    private boolean isHealth(int c) { return c >= 8 && c <= 11; }
    private boolean sameGroup(int a, int b) {
        return (isSkin(a) && isSkin(b)) || (isHair(a) && isHair(b)) || (isHealth(a) && isHealth(b));
    }

    private long countMatches(Set<Long> eids, Set<Long> wanted) {
        if (wanted == null || wanted.isEmpty() || eids == null || eids.isEmpty()) return 0;
        long cnt = 0;
        for (Long x : wanted) if (eids.contains(x)) cnt++;
        return cnt;
    }

    private List<ProductRecommendationDto> limitPerBrand(List<ProductRecommendationDto> list, int maxPerBrand) {
        Map<String, Integer> cnt = new HashMap<>();
        List<ProductRecommendationDto> res = new ArrayList<>();
        for (var p : list) {
            String brand = p.getBrandName() == null ? "" : p.getBrandName();
            int c = cnt.getOrDefault(brand, 0);
            if (c < maxPerBrand) { res.add(p); cnt.put(brand, c+1); }
        }
        return res;
    }

    private Integer asIntObj(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignore) {}
        }
        return null;
    }
    private Integer getInt(Map<String,Object> m, String k) { Object v = m.get(k); return v==null? null: ((Number)v).intValue(); }
    private Long getLong(Map<String,Object> m, String k) { Object v = m.get(k); return v==null? null: ((Number)v).longValue(); }
    private double getDouble(Map<String,Object> m, String k) { Object v = m.get(k); return v==null? 0.0: ((Number)v).doubleValue(); }
    private <T> T orDefault(T v, T defv) { return v == null ? defv : v; }

    private List<Long> weightedSampleWithoutReplacement(List<Map.Entry<Long,Double>> pool, int k) {
        if (k <= 0 || pool == null || pool.isEmpty()) return List.of();
        k = Math.min(k, pool.size());
        Random rnd = new Random(System.nanoTime());
        List<Map.Entry<Long,Double>> list = new ArrayList<>(pool);
        List<Long> picked = new ArrayList<>(k);

        double total = list.stream().mapToDouble(e -> Math.max(0.0, e.getValue())).sum();
        for (int t = 0; t < k && !list.isEmpty() && total > 0; t++) {
            double r = rnd.nextDouble() * total;
            double acc = 0.0;
            int idx = -1;
            for (int i = 0; i < list.size(); i++) {
                acc += Math.max(0.0, list.get(i).getValue());
                if (acc >= r) { idx = i; break; }
            }
            if (idx < 0) idx = list.size() - 1;
            picked.add(list.get(idx).getKey());
            total -= Math.max(0.0, list.get(idx).getValue());
            list.remove(idx);
        }
        return picked;
    }
}
