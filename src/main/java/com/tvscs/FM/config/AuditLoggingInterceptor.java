package com.tvscs.FM.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvscs.FM.models.TransactionLog;
import com.tvscs.FM.repository.TransactionLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interceptor that logs all HTTP requests/responses to fm_audit table.
 * Captures endpoint, HTTP method, status, duration, client info, and payloads.
 */
@Component
@Slf4j
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final TransactionLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final ThreadLocal<Long> requestStartTime = new ThreadLocal<>();

    public AuditLoggingInterceptor(TransactionLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestStartTime.set(System.currentTimeMillis());
        log.debug("[AUDIT] preHandle: uri={}, method={}, contentType={}",
                request.getRequestURI(), request.getMethod(), request.getContentType());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = requestStartTime.get();
        long durationMs = System.currentTimeMillis() - startTime;
        requestStartTime.remove();

        try {
            // Extract account info from request attributes
            String accountId = (String) request.getAttribute("auth.accountId");
            String portfolio = (String) request.getAttribute("auth.portfolio");
            String createdBy = accountId != null ? accountId : "public";

            // DEBUG: Log all request attributes
            log.debug("[AUDIT] Request attributes: auth.accountId={}, auth.portfolio={}",
                    accountId, portfolio);
            java.util.Enumeration<String> attrNames = request.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                log.debug("[AUDIT] Attribute: {} = {}", attrName, request.getAttribute(attrName));
            }

            // Capture request body
            String reqPayload = extractRequestPayload(request);
            log.debug("[AUDIT] Extracted request payload: {}",
                    reqPayload != null ? reqPayload.substring(0, Math.min(200, reqPayload.length())) + "..." : "NULL");

            // Capture response body
            String respPayload = extractResponsePayload(response);
            log.debug("[AUDIT] Extracted response payload: {}",
                    respPayload != null ? respPayload.substring(0, Math.min(200, respPayload.length())) + "..." : "NULL");

            // Extract transaction ID
            String fmTransactionId = extractTransactionId(request);
            log.debug("[AUDIT] Transaction ID: {}", fmTransactionId);

            // Build audit log entry
            TransactionLog auditLog = TransactionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .fmTransactionId(fmTransactionId)
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .payload(reqPayload)
                    .response(respPayload)
                    .httpStatus(response.getStatus())
                    .accountId(accountId)
                    .portfolio(portfolio)
                    .clientIp(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .requestDurationMs(durationMs)
                    .isError(ex != null || response.getStatus() >= 400 ? 1 : 0)
                    .errorMessage(ex != null ? truncateMessage(ex.getMessage()) : null)
                    .createdBy(createdBy)
                    .updatedBy(createdBy)
                    .build();

            // Save asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    auditLogRepository.save(auditLog);
                    log.info("[AUDIT] SAVED: endpoint={}, status={}, duration={}ms, payload={}, response={}",
                            auditLog.getEndpoint(),
                            auditLog.getHttpStatus(),
                            durationMs,
                            reqPayload != null ? "HAS_DATA" : "NULL",
                            respPayload != null ? "HAS_DATA" : "NULL");
                } catch (Exception e) {
                    log.error("[AUDIT] Failed to save audit log for {}: {}",
                            auditLog.getEndpoint(), e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("[AUDIT] Failed to create audit log entry: {}", e.getMessage(), e);
        }
    }

    private String extractRequestPayload(HttpServletRequest request) {
        log.debug("[AUDIT] extractRequestPayload: isWrapped={}, contentType={}, contentLength={}",
                RequestBodyCaptureFilter.isWrapped(),
                request.getContentType(),
                request.getContentLength());

        try {
            // FIRST: Check if controller set canonicalized payload (for multipart form data)
            String controllerPayload = (String) request.getAttribute("audit.payload");
            if (controllerPayload != null && !controllerPayload.isEmpty()) {
                log.debug("[AUDIT] Using controller-set canonicalized payload: {}",
                        controllerPayload.substring(0, Math.min(200, controllerPayload.length())));
                return truncatePayload(controllerPayload);
            }
            
            // SECOND: Check if body was captured by RequestBodyCaptureFilter (for JSON bodies)
            String capturedBody = RequestBodyCaptureFilter.getCachedBody();
            if (capturedBody != null && !capturedBody.isEmpty()) {
                log.debug("[AUDIT] Using captured body from CachingRequestWrapper: {}",
                        capturedBody.substring(0, Math.min(200, capturedBody.length())));
                return truncatePayload(capturedBody);
            }
            
            log.debug("[AUDIT] No payload found, returning null");
            return null;

        } catch (Exception e) {
            log.error("[AUDIT] Error extracting request payload: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractResponsePayload(HttpServletResponse response) {
        log.debug("[AUDIT] extractResponsePayload: isWrapper={}", response instanceof ContentCachingResponseWrapper);

        try {
            if (response instanceof ContentCachingResponseWrapper) {
                ContentCachingResponseWrapper wrappedResponse = (ContentCachingResponseWrapper) response;
                byte[] content = wrappedResponse.getContentAsByteArray();
                log.debug("[AUDIT] Response content length: {} bytes", content.length);

                if (content.length > 0) {
                    String payload = new String(content, StandardCharsets.UTF_8);
                    log.debug("[AUDIT] Response payload: {}",
                            payload.substring(0, Math.min(200, payload.length())));
                    return truncatePayload(payload);
                }
            } else {
                log.warn("[AUDIT] Response is NOT a ContentCachingResponseWrapper!");
            }
            return null;
        } catch (Exception e) {
            log.error("[AUDIT] Error extracting response payload: {}", e.getMessage(), e);
            return null;
        }
    }

    private String truncatePayload(String payload) {
        if (payload == null) return null;
        if (payload.length() > 10000) {
            return payload.substring(0, 10000) + "... [truncated]";
        }
        return payload;
    }

    private String truncateMessage(String message) {
        if (message == null) return null;
        if (message.length() > 4000) {
            return message.substring(0, 4000);
        }
        return message;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractTransactionId(HttpServletRequest request) {
        String kid = (String) request.getAttribute("auth.kid");
        if (kid != null) return kid;
        String webhookKid = (String) request.getAttribute("auth.webhookKid");
        if (webhookKid != null) return webhookKid;
        return null;
    }
}
