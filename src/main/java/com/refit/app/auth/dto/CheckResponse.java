package com.refit.app.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckResponse {

    private String status;
    private String message;
    private boolean available;
}
