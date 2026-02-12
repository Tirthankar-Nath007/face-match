package com.tvscs.FM.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvscs.FM.repository.ConfigFieldRepository;
import com.tvscs.FM.utils.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ApiKeyAndJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ConfigFieldRepository configFieldRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern for validating API key format (16 alphanumeric characters)
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9]{16}$");

    public ApiKeyAndJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ConfigFieldRepository configFieldRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.configFieldRepository = configFieldRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply this filter to /face-match endpoint
        if (!path.startsWith("/api/v1/face-match")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract and validate X-API-KEY header
            String apiKey = request.getHeader("X-API-KEY");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                sendUnauthorizedError(response, path, "Missing X-API-KEY header");
                return;
            }

            if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
                sendUnauthorizedError(response, path, "Invalid X-API-KEY format ");
                return;
            }

            // Extract and validate Authorization header (Bearer JWT)
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendUnauthorizedError(response, path, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // Validate JWT
            Claims claims;
            try {
                claims = jwtTokenProvider.validateAndGetClaims(token);
            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                sendUnauthorizedError(response, path, "Invalid or expired JWT token");
                return;
            }

            // Extract claims from JWT
            String tokenApiKey = (String) claims.get("apiKey");
            String accountId = (String) claims.get("accountId");
            String portfolio = (String) claims.get("portfolio");

            // Verify that the apiKey in token matches the header apiKey
            if (!apiKey.equals(tokenApiKey)) {
                sendUnauthorizedError(response, path, "X-API-KEY does not match JWT token");
                return;
            }

            // Verify that the account exists and is active in the database
            var configField = configFieldRepository.findByApiKeyAndIsActive(apiKey, 1);
            if (configField.isEmpty()) {
                log.warn("API key not found or not active: {}", apiKey);
                sendUnauthorizedError(response, path, "API key not found or inactive");
                return;
            }

            var cf = configField.get();
            if (!cf.getAccountId().equals(accountId)) {
                sendUnauthorizedError(response, path, "Account ID mismatch");
                return;
            }

            // Set request attributes for use in the controller
            request.setAttribute("auth.apiKey", apiKey);
            request.setAttribute("auth.accountId", accountId);
            request.setAttribute("auth.portfolio", portfolio);

            // Create authentication token and set it in SecurityContext
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(accountId, null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("JWT authentication successful for accountId: {}, portfolio: {}", accountId, portfolio);

            filterChain.doFilter(request, response);
            
        } catch (DataAccessException dbException) {
            // Database is down or connection pool is exhausted
            log.error("Database connection error in ApiKeyAndJwtAuthenticationFilter: {}", dbException.getMessage(), dbException);
            sendServiceUnavailableError(response, request.getRequestURI(), "Database service temporarily unavailable");
        } catch (Exception e) {
            log.error("Unexpected error in ApiKeyAndJwtAuthenticationFilter: {}", e.getMessage(), e);
            sendUnauthorizedError(response, request.getRequestURI(), "Authentication failed");
        }
    }

    private void sendUnauthorizedError(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", message);
        errorResponse.put("path", path);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void sendServiceUnavailableError(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("status", 503);
        errorResponse.put("error", "Service Unavailable");
        errorResponse.put("message", message);
        errorResponse.put("path", path);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
