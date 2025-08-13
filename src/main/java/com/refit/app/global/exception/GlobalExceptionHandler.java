package com.refit.app.global.exception;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RefitException.class)
    public ResponseEntity<ErrorResponse> handleZippickException(RefitException exception){
        ErrorResponse response = new ErrorResponse(exception.getCode(), exception.getMessage());
        log.error("Zippick Exception", exception);
        return new ResponseEntity<>(response, exception.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception){
        ErrorResponse response = new ErrorResponse(ErrorCode.INVALID_REQUEST_PARAMS.getCode(), errors(exception));
        log.error("MethodArgumentNotValidException", exception);
        return new ResponseEntity<>(response, ErrorCode.INVALID_REQUEST_PARAMS.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception){
        ErrorResponse resopnse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getMessage());
        log.error("IllegalArgumentException", exception);
        return new ResponseEntity<>(resopnse, ErrorCode.ILLEGAL_ARGUMENT.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception){
        ErrorResponse response = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), exception.getMessage());
        log.error("Exception", exception);
        return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }

    private String field(ObjectError error){
        return ((FieldError) error).getField();
    }

    private String message(ObjectError error){
        return ((FieldError) error).getField();
    }

    private String errors(MethodArgumentNotValidException exception){
        return exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> field(error) + ": " + message(error))
                .collect(Collectors.joining(", "));
    }
}
