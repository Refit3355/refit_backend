package com.refit.app.global.web;

import com.refit.app.domain.memberProduct.model.UsageStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UsageStatusConverter implements Converter<String, UsageStatus> {
    @Override
    public UsageStatus convert(String source) {
        if (source == null || source.isBlank()) return null;
        String s = source.trim();
        if ("all".equalsIgnoreCase(s)) return null;
        return UsageStatus.valueOf(s.toUpperCase());
    }
}