package com.refit.app.domain.memberProduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UsageStatus {
    USING(1), COMPLETED(2), DELETED(3);

    private final int code;
    UsageStatus(int code){ this.code = code; }
    public int getCode(){ return code; }

    @JsonValue
    public String toJson(){ return name().toLowerCase(); }

    @JsonCreator
    public static UsageStatus fromJson(String v){
        return UsageStatus.valueOf(v.toUpperCase());
    }

    public static UsageStatus fromCode(int code){
        return switch (code) {
            case 1 -> USING;
            case 2 -> COMPLETED;
            case 3 -> DELETED;
            default -> throw new IllegalArgumentException("Unknown code: " + code);
        };
    }
}