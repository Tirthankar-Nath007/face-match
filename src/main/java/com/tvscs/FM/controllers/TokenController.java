package com.tvscs.FM.controllers;

import com.tvscs.FM.services.TokenService;
import com.tvscs.FM.utils.ResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class TokenController {

    private final TokenService tokenService;
    private final ResponseBuilder responseBuilder;

    public TokenController(TokenService tokenService, ResponseBuilder responseBuilder) {
        this.tokenService = tokenService;
        this.responseBuilder = responseBuilder;
    }

    /**
     * Generate a JWT token given valid API key and account ID.
     * Public endpoint (no authentication required).
     *
     * Request Body:
     * {
     *   "apiKey": "abcd1234efgh5678",
     *   "accountId": "123456789"
     * }
     *
     * Response:
     * {
     *   "token": "eyJhbGc...",
     *   "expiresAt": "2024-02-10T12:30:00Z"
     * }
     */
    @PostMapping(value = "/generate-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String apiKey = request.get("apiKey");
            String accountId = request.get("accountId");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(responseBuilder.badRequest("apiKey is required", null, httpRequest));
            }

            if (accountId == null || accountId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(responseBuilder.badRequest("accountId is required", null, httpRequest));
            }

            Map<String, Object> tokenResponse = tokenService.generateToken(apiKey.trim(), accountId.trim());
            log.info("Token generated successfully for accountId: {}", accountId);
            return ResponseEntity.ok(responseBuilder.success(tokenResponse, "Token generated successfully", httpRequest));

        } catch (IllegalArgumentException ex) {
            log.warn("Bad request for token generation: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(responseBuilder.badRequest(ex.getMessage(), null, httpRequest));
        } catch (Exception ex) {
            log.error("Error generating token", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBuilder.serverError("Token generation failed", ex.getMessage(), httpRequest));
        }
    }
}
