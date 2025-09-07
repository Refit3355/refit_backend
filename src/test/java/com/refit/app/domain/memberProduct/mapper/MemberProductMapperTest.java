package com.refit.app.domain.memberProduct.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import com.refit.app.domain.memberProduct.dto.response.MemberProductDetailResponse;
import com.refit.app.testsupport.OracleTC;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(OracleTC.class)
@Sql(
        scripts = "/schema-test-oracle.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
        config = @SqlConfig(separator = "/")
)
@Sql(
        scripts = "/data-oracle.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MemberProductMapperTest {

    @Autowired
    MemberProductMapper mapper;

    Long memberId;

    @BeforeEach
    void init() {
        memberId = 42L;
    }

    @Test
    void findProductSimple_returnsRow_whenNotDeleted() {
        ProductSimpleRow row = mapper.findProductSimple(10L);
        assertThat(row).isNotNull();
        assertThat(row.getProductId()).isEqualTo(10L);
        assertThat(row.getProductName()).isEqualTo("앰플A");
        assertThat(row.getBrandName()).isEqualTo("브랜드A");
        assertThat(row.getRecommendedPeriod()).isEqualTo(90);
        assertThat(row.getBhType()).isEqualTo(1);
        assertThat(row.getCategoryId()).isEqualTo(100L);
    }

    @Test
    void findProductSimple_skipsDeleted() {
        ProductSimpleRow row = mapper.findProductSimple(20L); // DELETED_AT not null
        assertThat(row).isNull();
    }

    @Test
    void insertMemberProduct_insertsRow_withSequenceAndDerivedColumns() {
        mapper.insertMemberProduct(
                memberId,
                10L,
                LocalDate.of(2025, 8, 1),
                90,
                1,
                "앰플A",
                "브랜드A",
                1,
                100L
        );
        // 목록 조회로 간접 검증
        List<MemberProductDetailResponse> list =
                mapper.selectMemberProducts(memberId, 1, null);
        assertThat(list).hasSize(1);
        MemberProductDetailResponse d = list.get(0);
        assertThat(d.getProductId()).isEqualTo(10L);
        assertThat(d.getBrandName()).isEqualTo("브랜드A");
        assertThat(d.getProductName()).isEqualTo("앰플A");
        assertThat(d.getRecommendedPeriod()).isEqualTo(90);
        assertThat(d.getCategoryId()).isEqualTo(100L);
        // effects는 EFFECT_ID 리스트이므로 비어있지 않으면 OK
        assertThat(d.getEffects()).isNotNull();
        assertThat(d.getEffects()).isNotEmpty();
    }

    @Test
    void insertMemberProductWithEffects_inserts_mp_and_effects() {
        Long id = mapper.insertMemberProductWithEffects(
                memberId,
                null, // 커스텀(외부) 상품
                LocalDate.of(2025, 8, 10),
                30,
                1,
                "커스텀제품",
                "브랜드X",
                1,
                100L,
                List.of(1L, 2L) // EFFECT_ID: 1=보습, 2=진정
        );
        assertThat(id).isNotNull();

        List<MemberProductDetailResponse> list =
                mapper.selectMemberProducts(memberId, 1, 1 /* USING */);
        assertThat(list).hasSize(1);
        MemberProductDetailResponse d = list.get(0);

        assertThat(d.getProductId()).isNull(); // 외부 상품은 product_id 없음
        assertThat(d.getBrandName()).isEqualTo("브랜드X");
        assertThat(d.getProductName()).isEqualTo("커스텀제품");
        // 효과는 ID 리스트를 검증
        assertThat(d.getEffects()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void markCompleted_and_markUsing_toggleStatus() {
        // 1) in-app 상품 하나 생성(USING=1)
        mapper.insertMemberProduct(
                memberId,
                10L,
                LocalDate.of(2025, 8, 1),
                90,
                1,
                "앰플A",
                "브랜드A",
                1,
                100L
        );
        var list1 = mapper.selectMemberProducts(memberId, 1, 1);
        assertThat(list1).hasSize(1);
        Long mpId = list1.get(0).getMemberProductId();

        // 2) COMPLETED로 변경
        int u1 = mapper.markCompleted(memberId, mpId);
        assertThat(u1).isNotZero();

        var list2 = mapper.selectMemberProducts(memberId, 1, 2);
        assertThat(list2).hasSize(1);

        // 3) 다시 USING으로
        int u2 = mapper.markUsing(memberId, mpId);
        assertThat(u2).isNotZero();

        var list3 = mapper.selectMemberProducts(memberId, 1, 1);
        assertThat(list3).hasSize(1);
    }

    @Test
    void softDeleteMemberProduct_setsDeletedAt_andFiltersOut() {
        mapper.insertMemberProduct(
                memberId,
                10L,
                LocalDate.of(2025, 8, 1),
                90,
                1,
                "앰플A",
                "브랜드A",
                1,
                100L
        );
        var before = mapper.selectMemberProducts(memberId, 1, null);
        assertThat(before).hasSize(1);
        Long mpId = before.get(0).getMemberProductId();

        int updated = mapper.softDeleteMemberProduct(memberId, mpId);
        assertThat(updated).isNotZero();

        var after = mapper.selectMemberProducts(memberId, 1, null);
        assertThat(after).isEmpty();
    }

    @Test
    void updateInAppMemberProduct_updatesOnly_whenProductIdNotNull_andUsing() {
        // in-app 생성
        mapper.insertMemberProduct(
                memberId,
                10L,
                LocalDate.of(2025, 8, 1),
                90,
                1,
                "앰플A",
                "브랜드A",
                1,
                100L
        );
        var rows = mapper.selectMemberProducts(memberId, 1, 1);
        Long mpId = rows.get(0).getMemberProductId();

        int cnt = mapper.updateInAppMemberProduct(
                memberId, mpId, 120, "2025-08-05"
        );
        assertThat(cnt).isNotZero();

        var after = mapper.selectMemberProducts(memberId, 1, 1);
        assertThat(after.get(0).getRecommendedPeriod()).isEqualTo(120);
        assertThat(after.get(0).getStartDate()).isEqualTo("2025-08-05");
    }

    @Test
    void updateExternalMemberProduct_updates_and_effects_replace() {
        // external 생성
        Long id = mapper.insertMemberProductWithEffects(
                memberId, null, LocalDate.of(2025, 8, 1), 30, 1,
                "커스텀A", "브X", 1, 100L, List.of(1L)
        );
        var rows = mapper.selectMemberProducts(memberId, 1, 1);
        Long mpId = rows.get(0).getMemberProductId();

        // 효과 전체 교체 (1 -> 2만)
        int cnt = mapper.updateExternalMemberProduct(
                memberId, mpId,
                "커스텀A-수정", "브X-수정",
                45, "2025-08-03", 100L
        );
        assertThat(cnt).isNotZero();

        // deleteAll → insertEffect (직접 호출)
        mapper.deleteAllEffects(mpId);
        mapper.insertEffect(memberId, mpId, 2L);

        var after = mapper.selectMemberProducts(memberId, 1, 1);
        var d = after.get(0);
        assertThat(d.getProductId()).isNull();
        assertThat(d.getProductName()).isEqualTo("커스텀A-수정");
        assertThat(d.getBrandName()).isEqualTo("브X-수정");
        assertThat(d.getRecommendedPeriod()).isEqualTo(45);
        assertThat(d.getStartDate()).isEqualTo("2025-08-03");
        // 이름이 아니라 EFFECT_ID로 검증
        assertThat(d.getEffects()).containsExactly(2L);
    }
}
