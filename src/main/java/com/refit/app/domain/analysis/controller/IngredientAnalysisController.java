package com.refit.app.domain.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.analysis.dto.response.AnalysisResponseDto;
import com.refit.app.domain.analysis.service.AnalysisService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/analysis")
public class IngredientAnalysisController {

    private final ChatClient chat;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AnalysisService analysisService;

    public IngredientAnalysisController(ChatClient.Builder builder,
            AnalysisService analysisService) {
        this.chat = builder.build();
        this.analysisService = analysisService;
    }

    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AnalysisResponseDto analyze(
            Authentication authentication,
            @RequestPart("image") MultipartFile image,
            @RequestParam("productType") String productType // "화장품" | "영양제" | "beauty" | "health"
    ) throws Exception {
        Long memberId = (Long) authentication.getPrincipal();

        return analysisService.analyzeImage(
                memberId,
                image.getBytes(),
                productType,
                image.getOriginalFilename(),
                image.getContentType()
        );
    }
}
