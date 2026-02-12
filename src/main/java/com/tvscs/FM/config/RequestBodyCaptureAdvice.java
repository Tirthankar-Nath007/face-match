package com.tvscs.FM.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * RequestBodyAdvice to capture raw request body for audit logging.
 * This intercepts @RequestBody processing and stores the raw JSON for later retrieval.
 */
@ControllerAdvice
public class RequestBodyCaptureAdvice implements RequestBodyAdvice {

    // ThreadLocal to store the raw request body for audit logging
    private static final ThreadLocal<String> rawRequestBody = new ThreadLocal<>();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                           Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                          Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                               Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                 Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * Capture the raw request body for audit logging.
     * This should be called BEFORE @RequestBody processing reads the body.
     */
    public static void captureRawBody(String rawBody) {
        if (rawBody != null) {
            rawRequestBody.set(rawBody);
        }
    }

    /**
     * Get the captured raw request body.
     * Returns null if no body was captured.
     */
    public static String getRawRequestBody() {
        return rawRequestBody.get();
    }

    /**
     * Clear the captured raw request body.
     * Should be called after audit logging is complete.
     */
    public static void clearRawRequestBody() {
        rawRequestBody.remove();
    }
}
