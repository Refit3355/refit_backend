package com.refit.app.domain.memberProduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductType {
    BEAUTY(0), HEALTH(1);

    private final int code;
    ProductType(int code){ this.code = code; }
    public int getCode(){ return code; }

    @JsonValue
    public String toJson(){ return name().toLowerCase(); }

    @JsonCreator
    public static ProductType fromJson(String v){
        return ProductType.valueOf(v.toUpperCase());
    }
}
