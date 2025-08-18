package com.refit.app.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UtilResponse<T> {

    private String status;
    private String message;
    private T data;
}
