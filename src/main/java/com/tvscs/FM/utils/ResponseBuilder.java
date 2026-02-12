package com.tvscs.FM.utils;

import com.tvscs.FM.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class ResponseBuilder {

    public <T> ApiResponse<T> success(T data, String message, HttpServletRequest request) {
        return ApiResponse.<T>builder()
                .StatusCode(200)
                .TimeStamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .message(message)
                .Data(data)
                .build();
    }

    public <T> ApiResponse<T> created(T data, String message, HttpServletRequest request) {
        return ApiResponse.<T>builder()
                .StatusCode(201)
                .TimeStamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .message(message)
                .Data(data)
                .build();
    }

    public <T> ApiResponse<T> error(Integer statusCode, String message, String errorDetail, HttpServletRequest request) {
        return ApiResponse.<T>builder()
                .StatusCode(statusCode)
                .TimeStamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .message(message)
                .error_detail(errorDetail)
                .build();
    }

    public <T> ApiResponse<T> badRequest(String message, String errorDetail, HttpServletRequest request) {
        return error(400, message, errorDetail, request);
    }

    public <T> ApiResponse<T> notFound(String message, HttpServletRequest request) {
        return error(404, message, null, request);
    }

    public <T> ApiResponse<T> serverError(String message, String errorDetail, HttpServletRequest request) {
        return error(500, message, errorDetail, request);
    }
}
