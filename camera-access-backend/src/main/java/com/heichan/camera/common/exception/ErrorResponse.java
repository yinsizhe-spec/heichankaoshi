package com.heichan.camera.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ErrorResponse {

    private ErrorDetail error;

    @Data
    @Builder
    public static class ErrorDetail {

        private String code;

        private String message;

        private OffsetDateTime timestamp;
    }
}