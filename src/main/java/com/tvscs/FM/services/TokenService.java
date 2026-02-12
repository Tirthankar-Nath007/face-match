package com.tvscs.FM.services;

import com.tvscs.FM.models.ConfigFields;
import com.tvscs.FM.repository.ConfigFieldRepository;
import com.tvscs.FM.utils.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final ConfigFieldRepository configFieldRepository;

    // Patterns for validation
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9]{16}$");
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{9}$");

    public TokenService(JwtTokenProvider jwtTokenProvider, ConfigFieldRepository configFieldRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.configFieldRepository = configFieldRepository;
    }

    /**
     * Generate a JWT token for the given API key and account ID.
     * Validates that the key exists and is active, then issues a short-lived JWT.
     *
     * @param apiKey    The API key to authenticate
     * @param accountId The account ID to include in token
     * @return Map with "token" and "expiresAt" fields
     * @throws IllegalArgumentException if validation fails
     */
    public Map<String, Object> generateToken(String apiKey, String accountId) {
        // Validate format
        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            throw new IllegalArgumentException("Invalid API key format ");
        }

        if (!ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
            throw new IllegalArgumentException("Invalid account ID format (must be 9 alphanumeric characters)");
        }

        // Check if API key exists and is active
        var configField = configFieldRepository.findByApiKeyAndIsActive(apiKey, 1);
        if (configField.isEmpty()) {
            log.warn("API key not found or not active: {}", apiKey);
            throw new IllegalArgumentException("API key not found or inactive");
        }

        ConfigFields cf = configField.get();

        // Verify accountId matches
        if (!cf.getAccountId().equals(accountId)) {
            log.warn("Account ID mismatch for API key: {} expected: {}, got: {}", apiKey, cf.getAccountId(), accountId);
            throw new IllegalArgumentException("Account ID mismatch");
        }

        // Create JWT claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("apiKey", apiKey);
        claims.put("accountId", accountId);
        claims.put("portfolio", cf.getPortfolio());

        // Generate token
        String token = jwtTokenProvider.generateToken(claims);
        java.util.Date expiryTime = jwtTokenProvider.getExpirationTime(token);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiresAt", expiryTime);

        log.info("Token generated successfully for accountId: {}", accountId);

        return response;
    }
}
