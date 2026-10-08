package com.flock.urja.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortalException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse portalError(PortalException e) {
        return new ErrorResponse("UPSTREAM_ERROR", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(IllegalArgumentException e) {
        return new ErrorResponse("BAD_REQUEST", e.getMessage());
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
