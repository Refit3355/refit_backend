package com.refit.app.domain.product.model;

import java.util.Arrays;

public enum SortType {
    LATEST("latest"),
    SALES("sales"),
    PRICE_DESC("price_desc"),
    PRICE_ASC("price_asc");

    private final String code;
    SortType(String code) { this.code = code; }
    public String getCode() { return code; }

    public static SortType fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid sort type: " + code));
    }
}

