package com.heichan.camera.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        ErrorResponse response = buildError(
                exception.getCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    /**
     * 处理 @Valid 参数校验异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .orElse(null);

        String message = fieldError == null
                ? "请求参数不正确"
                : fieldError.getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(buildError("VALIDATION_ERROR", message));
    }

    /**
     * 处理系统未知异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception
    ) {
        exception.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        "INTERNAL_SERVER_ERROR",
                        "服务器内部错误"
                ));
    }

    private ErrorResponse buildError(String code, String message) {
        return ErrorResponse.builder()
                .error(
                        ErrorResponse.ErrorDetail.builder()
                                .code(code)
                                .message(message)
                                .timestamp(OffsetDateTime.now())
                                .build()
                )
                .build();
    }
}