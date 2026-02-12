package com.tvscs.FM.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvscs.FM.models.ConfigFields;
import com.tvscs.FM.repository.ConfigFieldRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
@Slf4j
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiKeyFilter.class);
    private final ConfigFieldRepository configFieldRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminApiKeyFilter(ConfigFieldRepository configFieldRepository) {
        this.configFieldRepository = configFieldRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply this filter to admin endpoints
        if (!path.startsWith("/api/v1/create-account") && !path.startsWith("/api/v1/update-account")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader("X-Admin-API-KEY");
        
        if (providedKey == null || providedKey.trim().isEmpty()) {
            logger.warn("Missing X-Admin-API-KEY header on path: {}", path);
            sendUnauthorizedError(response, path, "Missing X-Admin-API-KEY header");
            return;
        }

        try {
            // Verify admin API key from database (portfolio = "Admin", is_active = 1)
            var adminConfig = configFieldRepository.findByPortfolioAndIsActive("Admin", 1);
            
            if (adminConfig.isEmpty()) {
                logger.error("Admin configuration not found in database");
                sendUnauthorizedError(response, path, "Admin configuration not found");
                return;
            }

            ConfigFields adminConfigFields = adminConfig.get();
            String expectedAdminKey = adminConfigFields.getApiKey();

            if (!providedKey.equals(expectedAdminKey)) {
                logger.warn("Invalid admin API key attempt on path: {}", path);
                sendUnauthorizedError(response, path, "Invalid or missing X-Admin-API-KEY header");
                return;
            }

            // Create authentication token and set it in SecurityContext
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken("admin", null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            logger.debug("Admin API key authentication successful");
            filterChain.doFilter(request, response);
            
        } catch (DataAccessException dbException) {
            // Database is down or connection pool is exhausted
            logger.error("Database connection error in AdminApiKeyFilter: {}", dbException.getMessage(), dbException);
            sendServiceUnavailableError(response, path, "Database service temporarily unavailable");
        } catch (Exception ex) {
            // Unexpected error
            logger.error("Unexpected error in AdminApiKeyFilter: {}", ex.getMessage(), ex);
            sendServiceUnavailableError(response, path, "Service temporarily unavailable");
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

