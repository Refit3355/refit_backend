package com.refit.app.global.web;

import com.refit.app.domain.memberProduct.model.ProductType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProductTypeConverter implements Converter<String, ProductType> {
    @Override
    public ProductType convert(String source) {
        if (source == null) return null;
        return ProductType.valueOf(source.trim().toUpperCase());
    }
}