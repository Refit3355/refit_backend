package com.refit.app.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RefitException extends RuntimeException {
    private final String code;
    private final String message;
    private final HttpStatus status;

    public RefitException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public RefitException(ErrorCode errorCode, String message) {
        this(null, errorCode, message);
    }

    public RefitException(Throwable cause, ErrorCode errorCode) {
        this(cause, errorCode.getCode(), errorCode.getMessage(), errorCode.getStatus());
    }

    public RefitException(Throwable cause, ErrorCode errorCode, String message) {
        this(cause, errorCode.getCode(), message, errorCode.getStatus());
    }

    public RefitException(Throwable cause, String code, String message, HttpStatus status) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
