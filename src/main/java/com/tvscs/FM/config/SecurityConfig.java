package com.tvscs.FM.config;

import com.tvscs.FM.security.AdminApiKeyFilter;
import com.tvscs.FM.security.ApiKeyAndJwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final AdminApiKeyFilter adminApiKeyFilter;
    private final ApiKeyAndJwtAuthenticationFilter apiKeyAndJwtAuthenticationFilter;
    private final AuditLoggingInterceptor auditLoggingInterceptor;

    public SecurityConfig(AdminApiKeyFilter adminApiKeyFilter,
                         ApiKeyAndJwtAuthenticationFilter apiKeyAndJwtAuthenticationFilter,
                         AuditLoggingInterceptor auditLoggingInterceptor) {
        this.adminApiKeyFilter = adminApiKeyFilter;
        this.apiKeyAndJwtAuthenticationFilter = apiKeyAndJwtAuthenticationFilter;
        this.auditLoggingInterceptor = auditLoggingInterceptor;
    }

    /**
     * ContentCachingFilter that wraps requests/responses for audit logging.
     * This MUST be added FIRST in the Spring Security filter chain.
     */
    @Bean
    public OncePerRequestFilter contentCachingFilter() {
        return new OncePerRequestFilter() {
            private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);
            
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain)
                    throws ServletException, IOException {
                
                log.debug("[CACHE-FILTER] Before wrapping: uri={}, contentType={}", 
                        request.getRequestURI(), request.getContentType());
                
                // Wrap request and response for content caching
                // ContentCachingRequestWrapper caches the body when read (multiple times)
                ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 65536);
                ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
                
                log.debug("[CACHE-FILTER] After wrapping: wrappedRequest class={}", 
                        wrappedRequest.getClass().getName());
                
                try {
                    filterChain.doFilter(wrappedRequest, wrappedResponse);
                } finally {
                    // Copy cached response body to actual response
                    wrappedResponse.copyBodyToResponse();
                    log.debug("[CACHE-FILTER] Response body copied, cached length={}", 
                            wrappedResponse.getContentSize());
                }
            }
            
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                // Apply to all API requests
                boolean shouldFilter = request.getRequestURI().startsWith("/api/");
                log.debug("[CACHE-FILTER] shouldNotFilter: uri={}, shouldFilter={}", 
                        request.getRequestURI(), shouldFilter);
                return !shouldFilter;
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OncePerRequestFilter contentCachingFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/v1/generate-token").permitAll()
                        .requestMatchers("/api/v1/webhook").permitAll()
                        // Admin-protected endpoints (X-Admin-API-KEY required)
                        .requestMatchers("/api/v1/create-account").authenticated()
                        .requestMatchers("/api/v1/update-account/**").authenticated()
                        // API-key + JWT protected endpoints
                        .requestMatchers("/api/v1/face-match").authenticated()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // Add ContentCachingFilter FIRST in the chain
                .addFilterBefore(contentCachingFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAndJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminApiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(auditLoggingInterceptor)
                .addPathPatterns("/api/v1/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
