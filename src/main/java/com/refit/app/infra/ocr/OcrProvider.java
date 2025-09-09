package com.refit.app.infra.ocr;

public interface OcrProvider {
    /** 이미지 전체 OCR (줄바꿈 보존) */
    String fullText(byte[] imageBytes, String filename, String contentType);

    /** 전성분 블록만 따로 뽑고 싶을 때(선택): 기본은 빈 문자열 리턴 */
    default String ingredientBlock(byte[] imageBytes, String filename, String contentType) {
        return "";
    }
}