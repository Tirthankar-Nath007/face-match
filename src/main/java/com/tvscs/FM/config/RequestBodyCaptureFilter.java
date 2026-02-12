package com.tvscs.FM.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Filter that wraps requests in CachingRequestWrapper to allow multiple reads of the body.
 * This MUST run before any filter that reads the request body (like Spring Security).
 */
@Component
@Order(1)
public class RequestBodyCaptureFilter implements Filter {

    // ThreadLocal to store the wrapped request for later use
    private static final ThreadLocal<HttpServletRequest> wrappedRequest = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Only wrap POST/PUT/PATCH requests with content
        String method = httpRequest.getMethod();
        String contentType = httpRequest.getContentType();
        
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))
                && contentType != null && contentType.contains("application/json")) {
            
            try {
                // Wrap the request - this caches the body and allows multiple reads
                CachingRequestWrapper cachedRequest = new CachingRequestWrapper(httpRequest);
                wrappedRequest.set(cachedRequest);
                
                // Continue the filter chain with the wrapped request
                chain.doFilter(cachedRequest, response);
            } catch (IOException | ServletException e) {
                // If wrapping fails, continue with original request
                chain.doFilter(request, response);
            } finally {
                wrappedRequest.remove();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
    
    /**
     * Get the cached request body for the current thread.
     */
    public static String getCachedBody() {
        HttpServletRequest req = wrappedRequest.get();
        if (req instanceof CachingRequestWrapper) {
            byte[] body = ((CachingRequestWrapper) req).getCachedBody();
            if (body != null) {
                return new String(body, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    /**
     * Check if there's a wrapped request for the current thread.
     */
    public static boolean isWrapped() {
        HttpServletRequest req = wrappedRequest.get();
        return req instanceof CachingRequestWrapper;
    }
}
