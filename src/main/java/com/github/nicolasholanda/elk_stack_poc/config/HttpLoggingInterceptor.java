package com.github.nicolasholanda.elk_stack_poc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class HttpLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";
    private static final String REQUEST_ID = "requestId";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        request.setAttribute(REQUEST_ID, generateRequestId());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        long startTime = (long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;
        String requestId = (String) request.getAttribute(REQUEST_ID);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        // Log HTTP request with status code and duration
        if (status >= 500) {
            log.error("HTTP {} {} - Status: {} - Duration: {}ms - RequestID: {}",
                method, uri, status, duration, requestId);
        } else if (status >= 400) {
            log.warn("HTTP {} {} - Status: {} - Duration: {}ms - RequestID: {}",
                method, uri, status, duration, requestId);
        } else {
            log.info("HTTP {} {} - Status: {} - Duration: {}ms - RequestID: {}",
                method, uri, status, duration, requestId);
        }

        if (ex != null) {
            log.error("Request failed with exception - RequestID: {} - Message: {}", requestId, ex.getMessage(), ex);
        }
    }

    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + Thread.currentThread().threadId();
    }
}

