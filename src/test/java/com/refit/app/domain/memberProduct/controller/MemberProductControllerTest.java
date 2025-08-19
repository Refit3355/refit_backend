package com.refit.app.domain.memberProduct.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.dto.request.MemberProductUpdateRequest;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.memberProduct.model.UsageStatus;
import com.refit.app.domain.memberProduct.service.MemberProductService;
import com.refit.app.global.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = MemberProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.refit.app.global.config.JwtAuthFilter.class
        )
)
@TestPropertySource(properties = "spring.main.web-application-type=servlet") // WebFlux 차단
@AutoConfigureMockMvc(addFilters = false)
class MemberProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MemberProductService memberProductService;

    @Autowired
    ObjectMapper objectMapper;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentMemberId).thenReturn(42L);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Test
    void createFromProduct_shouldReturn204_andDelegateToService() throws Exception {
        mockMvc.perform(post("/member-products")
                        .param("productId", "100"))
                .andExpect(status().isNoContent());

        verify(memberProductService).createFromProduct(42L, 100L);
    }

    @Test
    void createCustom_shouldReturn204_andDelegateToService() throws Exception {
        MemberProductCreateRequest req = new MemberProductCreateRequest();

        mockMvc.perform(post("/member-products/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(memberProductService).createCustom(eq(42L), any(MemberProductCreateRequest.class));
    }

    @Test
    void getMemberProducts_shouldReturn200_andDelegateToService() throws Exception {
        mockMvc.perform(get("/member-products")
                        .param("type", ProductType.values()[0].name())
                        .param("status", UsageStatus.USING.name()))
                .andExpect(status().isOk());

        verify(memberProductService).getMemberProducts(42L, ProductType.values()[0],
                UsageStatus.USING);
    }

    @Test
    void deleteMemberProduct_shouldReturn204_andDelegateToService() throws Exception {
        mockMvc.perform(delete("/member-products/{id}", 777))
                .andExpect(status().isNoContent());

        verify(memberProductService).deleteMemberProduct(42L, 777L);
    }

    @Test
    void updateStatus_shouldReturn204_andDelegateToService() throws Exception {
        mockMvc.perform(patch("/member-products/{id}/status", 11)
                        .param("status", UsageStatus.COMPLETED.name()))
                .andExpect(status().isNoContent());

        verify(memberProductService).updateStatus(42L, 11L, UsageStatus.COMPLETED);
    }

    @Test
    void updateMemberProduct_shouldReturn204_andDelegateToService() throws Exception {
        MemberProductUpdateRequest req = new MemberProductUpdateRequest();

        mockMvc.perform(patch("/member-products/{id}", 55)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(memberProductService).updateMemberProduct(eq(42L), eq(55L),
                any(MemberProductUpdateRequest.class));
    }
}
