package com.tvscs.FM.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${fm.jwt.secret:faceMatchAppSecureJWTKeyChangeInProduction123456789}")
    private String jwtSecret;

    @Value("${fm.jwt.ttl-minutes:15}")
    private int jwtTtlMinutes;

    /**
     * Generate a JWT token with the given claims.
     *
     * @param claims Map of claims to include in the token (e.g., apiKey, accountId, portfolio)
     * @return JWT token string
     */
    public String generateToken(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        long expiryTime = now + (long) jwtTtlMinutes * 60 * 1000;

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiryTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate and extract claims from a JWT token.
     *
     * @param token JWT token string
     * @return Claims parsed from the token
     * @throws JwtException if token is invalid or expired
     */
    public Claims validateAndGetClaims(String token) throws JwtException {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract expiration time from a JWT token.
     *
     * @param token JWT token string
     * @return Expiration time as Date
     */
    public Date getExpirationTime(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.getExpiration();
        } catch (JwtException e) {
            logger.error("Error extracting expiration from token", e);
            return null;
        }
    }
}
