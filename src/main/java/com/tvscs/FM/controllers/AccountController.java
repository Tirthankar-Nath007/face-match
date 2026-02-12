package com.tvscs.FM.controllers;

import com.tvscs.FM.dto.ApiResponse;
import com.tvscs.FM.models.ConfigFields;
import com.tvscs.FM.services.AccountService;
import com.tvscs.FM.utils.ResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final ResponseBuilder responseBuilder;

    public AccountController(AccountService accountService, ResponseBuilder responseBuilder) {
        this.accountService = accountService;
        this.responseBuilder = responseBuilder;
    }

    /**
     * Create a new API key configuration.
     * Requires X-Admin-API-KEY header (enforced by AdminApiKeyFilter).
     * API Key and Account ID are auto-generated server-side.
     *
     * Request Body:
     * {
     *   "portfolio": "MyPortfolio"
     * }
     *
     * Headers:
     * - X-Admin-API-KEY: (from FM_CONFIGFIELDS where portfolio="Admin")
     * - X-Requested-By: <user creating account> (optional, defaults to "admin")
     *
     * Response: 201 Created with generated ConfigFields details including apiKey and accountId
     */
    @PostMapping(value = "/create-account", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAccount(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Requested-By", required = false, defaultValue = "admin") String createdBy,
            HttpServletRequest httpRequest
    ) {
        try {
            String portfolio = request.get("portfolio");

            if (portfolio == null || portfolio.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(responseBuilder.badRequest("Portfolio is required", null, httpRequest));
            }

            ConfigFields created = accountService.createAccount(
                    portfolio.trim(),
                    createdBy
            );

            log.info("Account created successfully: id={}, accountId={}, portfolio={}", 
                    created.getId(), created.getAccountId(), portfolio);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", created.getId());
            responseData.put("apiKey", created.getApiKey());
            responseData.put("accountId", created.getAccountId());
            responseData.put("portfolio", created.getPortfolio());
            responseData.put("isActive", created.getIsActive());
            responseData.put("createdAt", created.getCreatedAt());
            responseData.put("createdBy", created.getCreatedBy());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(responseBuilder.created(responseData, "Account created successfully", httpRequest));

        } catch (IllegalArgumentException ex) {
            log.warn("Bad request for account creation: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(responseBuilder.badRequest(ex.getMessage(), null, httpRequest));
        } catch (Exception ex) {
            log.error("Error creating account", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBuilder.serverError("Account creation failed", ex.getMessage(), httpRequest));
        }
    }

    /**
     * Update an existing API key configuration.
     * Requires X-Admin-API-KEY header (enforced by AdminApiKeyFilter).
     * Allows API key rotation, portfolio change, and is_active toggle.
     *
     * Path Parameter:
     * - accountId: The business account identifier (9-digit numeric)
     *
     * Request Body (all fields optional):
     * {
     *   "rotateKey": true,        // Auto-generate new API key
     *   "newApiKey": "customKey", // Custom API key (ignored if rotateKey is true)
     *   "portfolio": "NewPortfolio",
     *   "isActive": 0
     * }
     *
     * Headers:
     * - X-Admin-API-KEY: <configured admin key>
     * - X-Requested-By: <user performing update> (optional, defaults to "admin")
     *
     * Response: 200 OK with updated ConfigFields details
     */
    @PutMapping(value = "/update-account/{accountId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateAccount(
            @PathVariable String accountId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Requested-By", required = false, defaultValue = "admin") String updatedBy,
            HttpServletRequest httpRequest
    ) {
        try {
            // Extract rotateKey flag (optional)
            Boolean rotateKey = null;
            if (request.containsKey("rotateKey")) {
                Object rotateKeyObj = request.get("rotateKey");
                if (rotateKeyObj instanceof Boolean) {
                    rotateKey = (Boolean) rotateKeyObj;
                }
            }

            String newApiKey = (String) request.get("newApiKey");
            String portfolio = (String) request.get("portfolio");
            Integer isActive = null;
            if (request.containsKey("isActive")) {
                Object isActiveObj = request.get("isActive");
                if (isActiveObj instanceof Integer) {
                    isActive = (Integer) isActiveObj;
                } else if (isActiveObj instanceof Boolean) {
                    isActive = (Boolean) isActiveObj ? 1 : 0;
                }
            }

            ConfigFields updated = accountService.updateAccount(
                    accountId,
                    rotateKey,
                    newApiKey,
                    portfolio,
                    isActive,
                    updatedBy
            );

            log.info("Account updated successfully: accountId={}", accountId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updated.getId());
            responseData.put("apiKey", updated.getApiKey());
            responseData.put("accountId", updated.getAccountId());
            responseData.put("portfolio", updated.getPortfolio());
            responseData.put("isActive", updated.getIsActive());
            responseData.put("updatedAt", updated.getUpdatedAt());
            responseData.put("updatedBy", updated.getUpdatedBy());

            return ResponseEntity.ok(responseBuilder.success(responseData, "Account updated successfully", httpRequest));

        } catch (IllegalArgumentException ex) {
            log.warn("Bad request for account update: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(responseBuilder.badRequest(ex.getMessage(), null, httpRequest));
        } catch (Exception ex) {
            log.error("Error updating account", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBuilder.serverError("Account update failed", ex.getMessage(), httpRequest));
        }
    }
}
