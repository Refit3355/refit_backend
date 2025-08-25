package com.refit.app.domain.analysis.controller;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analysis")
public class IngredientAnalysisController {

    private final ChatClient chat;

    public IngredientAnalysisController(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @PostMapping(value = "/ingredients",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> analyzeIngredients(@RequestPart("file") MultipartFile file)
            throws Exception {
        byte[] bytes = file.getBytes();
        String mime = file.getContentType(); // e.g. image/jpeg

        // M6: Media 빌더 사용 가능
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(mime))
                .data(bytes)
                .build();

        String rsp = chat.prompt()
                .system("""
                            너는 화장품/영양제 전성분 라벨 분석가다.
                            이미지에서 '전성분' 항목만 읽어서 JSON으로만 출력해.
                            형식: {"ingredients":["정제수","부틸렌글라이콜", ...]}
                            설명/문장/코드블록 금지.
                        """)
                .user(u -> u.text("JSON만 반환.").media(media)) // ✅ M6에서 media(...)
                .call()
                .content();

        return Map.of("raw", rsp);
    }
}

