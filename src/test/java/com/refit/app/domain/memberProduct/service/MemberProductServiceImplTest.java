package com.refit.app.domain.memberProduct.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.refit.app.domain.memberProduct.dto.MetaRow;
import com.refit.app.domain.memberProduct.dto.ProductSimpleRow;
import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.dto.request.MemberProductUpdateRequest;
import com.refit.app.domain.memberProduct.mapper.MemberProductMapper;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.memberProduct.model.UsageStatus;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberProductServiceImplTest {

    @Mock
    MemberProductMapper mapper;

    @InjectMocks
    MemberProductServiceImpl service;

    @Test
    void createFromProduct_shouldThrow_whenProductNotFound() {
        RefitException ex = catchThrowableOfType(
                () -> service.createFromProduct(1L, 999L),
                RefitException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).contains("상품이 존재하지 않습니다");

        // code/status는 ErrorCode 값과 비교
        assertThat(ex.getCode()).isEqualTo(ErrorCode.ILLEGAL_ARGUMENT.getCode());
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.ILLEGAL_ARGUMENT.getStatus());
    }

    @Test
    void createFromProduct_shouldInsertWithDerivedFields() {
        ProductSimpleRow row = new ProductSimpleRow();
        row.setRecommendedPeriod(90);
        row.setProductName("앰플");
        row.setBrandName("브랜드");
        row.setBhType(1);
        row.setCategoryId(123L);

        when(mapper.findProductSimple(10L)).thenReturn(row);

        service.createFromProduct(7L, 10L);

        // insert 파라미터 검증
        ArgumentCaptor<Long> memberId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> productId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LocalDate> startDate = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Integer> recDays = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> usageStatus = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> productName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> brandName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> bhType = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> categoryId = ArgumentCaptor.forClass(Long.class);

        verify(mapper).insertMemberProduct(
                memberId.capture(),
                productId.capture(),
                startDate.capture(),
                recDays.capture(),
                usageStatus.capture(),
                productName.capture(),
                brandName.capture(),
                bhType.capture(),
                categoryId.capture()
        );

        assertThat(memberId.getValue()).isEqualTo(7L);
        assertThat(productId.getValue()).isEqualTo(10L);
        assertThat(recDays.getValue()).isEqualTo(90);
        assertThat(usageStatus.getValue()).isEqualTo(1); // USING
        assertThat(productName.getValue()).isEqualTo("앰플");
        assertThat(brandName.getValue()).isEqualTo("브랜드");
        assertThat(bhType.getValue()).isEqualTo(1);
        assertThat(categoryId.getValue()).isEqualTo(123L);
        assertThat(startDate.getValue()).isEqualTo(LocalDate.now());
    }

    @Test
    void createCustom_shouldInsertWithEffects_whenNullEffectHandled() {
        MemberProductCreateRequest req = new MemberProductCreateRequest();
        // 최소 필드 설정
        req.setType(ProductType.values()[0]);
        req.setProductName("커스텀");
        req.setBrandName("브랜드X");
        req.setStartDate(LocalDate.of(2025, 8, 1));
        req.setRecommendedPeriodDays(60);
        req.setCategoryId(99L);
        req.setEffect(null); // null 허용

        when(mapper.insertMemberProductWithEffects(anyLong(), isNull(), any(), any(), anyInt(),
                anyString(), anyString(), anyInt(), anyLong(), anyList()))
                .thenReturn(1234L);

        service.createCustom(10L, req);

        verify(mapper).insertMemberProductWithEffects(
                eq(10L),
                isNull(),
                eq(LocalDate.of(2025, 8, 1)),
                eq(60),
                eq(1),
                eq("커스텀"),
                eq("브랜드X"),
                anyInt(),
                eq(99L),
                eq(Collections.emptyList())
        );
    }

    @Test
    void getMemberProducts_shouldCallMapperWithCodes() {
        service.getMemberProducts(3L, ProductType.values()[0], UsageStatus.USING);
        verify(mapper).selectMemberProducts(eq(3L), anyInt(), eq(UsageStatus.USING.getCode()));
    }

    @Test
    void deleteMemberProduct_shouldThrow_whenUpdatedZero() {
        when(mapper.softDeleteMemberProduct(1L, 2L)).thenReturn(0);

        RefitException ex = catchThrowableOfType(
                () -> service.deleteMemberProduct(1L, 2L),
                RefitException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(ErrorCode.ILLEGAL_ARGUMENT.getCode());
    }

    @Test
    void updateStatus_shouldDispatchUsingOrCompleted_andThrowOnInvalid() {
        // USING
        when(mapper.markUsing(1L, 11L)).thenReturn(1);
        service.updateStatus(1L, 11L, UsageStatus.USING);
        verify(mapper).markUsing(1L, 11L);

        // COMPLETED
        when(mapper.markCompleted(1L, 12L)).thenReturn(1);
        service.updateStatus(1L, 12L, UsageStatus.COMPLETED);
        verify(mapper).markCompleted(1L, 12L);

        // null → 예외
        RefitException ex1 = catchThrowableOfType(
                () -> service.updateStatus(1L, 13L, null),
                RefitException.class
        );
        assertThat(ex1.getMessage()).contains("status 는 필수");

        // updated == 0 → 예외
        when(mapper.markUsing(1L, 14L)).thenReturn(0);
        RefitException ex2 = catchThrowableOfType(
                () -> service.updateStatus(1L, 14L, UsageStatus.USING),
                RefitException.class
        );
        assertThat(ex2.getMessage()).contains("대상이 없거나 상태 변경 불가");
    }

    @Test
    void updateMemberProduct_shouldThrow_whenRowNotFound() {
        when(mapper.findMemberProductMeta(1L, 100L)).thenReturn(null);

        RefitException ex = catchThrowableOfType(
                () -> service.updateMemberProduct(1L, 100L, new MemberProductUpdateRequest()),
                RefitException.class
        );

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND.getCode());
    }

    @Test
    void updateMemberProduct_shouldThrow_whenStatusNotUsing() {
        MetaRow row = new MetaRow();
        row.setMemberProductId(200L);
        row.setUsageStatus(UsageStatus.COMPLETED.getCode()); // USING 아님
        when(mapper.findMemberProductMeta(1L, 200L)).thenReturn(row);

        RefitException ex = catchThrowableOfType(
                () -> service.updateMemberProduct(1L, 200L, new MemberProductUpdateRequest()),
                RefitException.class
        );

        assertThat(ex.getCode()).isEqualTo(ErrorCode.STATUS_CONFLICT.getCode());
    }

    @Test
    void updateMemberProduct_shouldUpdateInApp_whenProductIdExists() {
        MetaRow row = new MetaRow();
        row.setMemberProductId(300L);
        row.setProductId(10L); // in-app product
        row.setUsageStatus(UsageStatus.USING.getCode());
        when(mapper.findMemberProductMeta(1L, 300L)).thenReturn(row);

        MemberProductUpdateRequest req = new MemberProductUpdateRequest();
        req.setRecommendedPeriod(60);

        service.updateMemberProduct(1L, 300L, req);

        verify(mapper).updateInAppMemberProduct(eq(1L), eq(300L), eq(60), any());
    }

    @Test
    void updateMemberProduct_shouldUpdateExternal_whenCustomProduct() {
        // given: 메타 데이터 - 커스텀 상품이고 USING 상태
        MetaRow row = new MetaRow();
        row.setMemberProductId(400L);
        row.setProductId(null); // custom product
        row.setUsageStatus(UsageStatus.USING.getCode());
        when(mapper.findMemberProductMeta(1L, 400L)).thenReturn(row);

        // request: 기존 정의에 맞춰 모두 세팅
        MemberProductUpdateRequest req = new MemberProductUpdateRequest();
        req.setProductName("수정된 이름");
        req.setBrandName("브X");
        req.setCategoryId(100L);
        req.setEffectIds(Arrays.asList(1L, 2L, 2L, null)); // 중복 + null 포함
        req.setRecommendedPeriod(45);
        req.setStartDate("2025-08-03");

        // when
        service.updateMemberProduct(1L, 400L, req);

        // then: 외부 상품 업데이트 호출 파라미터 정확성 검증
        verify(mapper).updateExternalMemberProduct(
                eq(1L),            // memberId
                eq(400L),          // memberProductId
                eq("수정된 이름"), // productName
                eq("브X"),         // brandName
                eq(45),            // recommendedPeriod
                eq("2025-08-03"),  // startDate
                eq(100L)           // categoryId
        );

        // 효과 전체 교체: 먼저 전삭제
        verify(mapper).deleteAllEffects(400L);

        // dedup + null 제거 후 순서 보존: 1, 2 두 번만 들어가야 함
        verify(mapper).insertEffect(1L, 400L, 1L);
        verify(mapper).insertEffect(1L, 400L, 2L);
        verify(mapper, times(2)).insertEffect(eq(1L), eq(400L), anyLong()); // 총 2회만
    }

}
