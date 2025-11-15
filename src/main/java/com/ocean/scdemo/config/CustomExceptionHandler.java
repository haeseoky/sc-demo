package com.ocean.scdemo.config;

import com.ocean.scdemo.aop.exception.DuplicateExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(DuplicateExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateExecutionException(DuplicateExecutionException e) {
        log.warn("Duplicate execution detected: {}", e.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("lockKey", e.getLockKey());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        log.error("Exception: ", e);
        return e.getMessage();
    }
}
