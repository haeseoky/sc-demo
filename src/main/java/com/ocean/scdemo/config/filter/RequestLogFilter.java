package com.ocean.scdemo.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        var requestId = UUID.randomUUID().toString();
        var contentCachingRequestWrapper = new ContentCachingRequestWrapper(request);
        var contentCachingResponseWrapper = new ContentCachingResponseWrapper(response);
        var startTime = System.currentTimeMillis();

        MDC.put("requestId", contentCachingRequestWrapper.getHeader("requestId"));
        filterChain.doFilter(request, response);

        var takeTime = System.currentTimeMillis() - startTime;
        contentCachingResponseWrapper.copyBodyToResponse();

        log.debug("Request: {} {} {} {}ms", request.getMethod(), request.getRequestURI(), request.getQueryString(), takeTime);
    }
}
