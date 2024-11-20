package com.ocean.scdemo.config;

import com.ocean.scdemo.config.model.CommonResponse;
import com.ocean.scdemo.config.model.ExceptionResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class CustomResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && (body instanceof ExceptionResponse || body instanceof String
            || body instanceof CommonResponse
            || request.getURI().getPath().contains("swagger")
            || request.getURI().getPath().contains("prometheus")
            || request.getURI().getPath().contains("api-docs"))) {
            return body;
        } else {
            return new CommonResponse<>(body);
        }
    }
}
