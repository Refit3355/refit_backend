package com.refit.app.domain.analysis.dto;

public enum AnalysisStatus {
    OK,
    UNSUPPORTED_IMAGE,   // 이미지 디코딩/포맷 문제
    NOT_PRODUCT_LABEL,   // 제품 라벨/성분 이미지가 아님
    NO_INGREDIENTS,      // 화장품 성분 섹션을 못 찾음
    LOW_TEXT,            // 텍스트가 너무 적음/읽기 어려움
    OCR_FAILURE,         // OCR 실패
    SERVER_ERROR         // 서버 내부 오류
}
