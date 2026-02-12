package com.tvscs.FM.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvscs.FM.config.DigioProperties;
import com.tvscs.FM.dto.ApiResponse;
import com.tvscs.FM.models.Transaction;
import com.tvscs.FM.repository.TransactionRepository;
import com.tvscs.FM.services.DigioService;
import com.tvscs.FM.services.DigioService.DigioResponse;
import com.tvscs.FM.utils.DigioUrlBuilder;
import com.tvscs.FM.utils.MultipartCanonicalizer;
import com.tvscs.FM.utils.ResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class FaceMatchController {

    private final DigioService digioService;
    private final DigioUrlBuilder digioUrlBuilder;
    private final DigioProperties digioProperties;
    private final TransactionRepository transactionRepository;
    private final ResponseBuilder responseBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FaceMatchController(DigioService digioService, DigioUrlBuilder digioUrlBuilder,
                               DigioProperties digioProperties,
                               TransactionRepository transactionRepository,
                               ResponseBuilder responseBuilder) {
        this.digioService = digioService;
        this.digioUrlBuilder = digioUrlBuilder;
        this.digioProperties = digioProperties;
        this.transactionRepository = transactionRepository;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping(value = "/face-match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> faceMatch(
            @RequestParam("customer_name") String customerName,
            @RequestParam("customer_identifier") String customerIdentifier,
            @RequestParam(value = "redirect_url", required = false, defaultValue = "false") String redirectFlag,
            @RequestPart("image") MultipartFile image,
            HttpServletRequest request
    ) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(responseBuilder.badRequest("Image file is required", null, request));
            }

            byte[] imageBytes = image.getBytes();
            String base64Image = DigioService.imageToBase64NoPrefix(imageBytes);
            log.debug("Image converted to base64 with length: {}", base64Image.length());

            boolean wantRedirect = "true".equalsIgnoreCase(redirectFlag);
            boolean notifyCustomer = !wantRedirect;

            DigioResponse digioResponse = digioService.createRequest(
                    customerName,
                    customerIdentifier,
                    base64Image,
                    null,
                    notifyCustomer
            );

            // Extract vendor_id (KID) from digioResponse for audit logging
            String kid = (String) digioResponse.getParsed().getOrDefault("id", null);
            if (kid != null) {
                request.setAttribute("auth.kid", kid);
            }

            // Canonicalize multipart form data to JSON for audit logging
            Map<String, Object> canonicalPayload = MultipartCanonicalizer.fromFaceMatchRequest(
                    customerName, customerIdentifier, redirectFlag, image, base64Image);
            String canonicalJson = objectMapper.writeValueAsString(canonicalPayload);
            request.setAttribute("audit.payload", canonicalJson);

            // Build redirect URL once if needed
            String redirectUrl = null;
            if (wantRedirect) {
                redirectUrl = digioUrlBuilder.build(digioResponse.getParsed(), digioProperties.getCallbackUrl());
            }

            // Return original response
            if (wantRedirect && redirectUrl != null) {
                return ResponseEntity.ok(responseBuilder.success(Map.of("redirect_url", redirectUrl),
                        "Redirect URL generated", request));
            }

            return ResponseEntity.ok(responseBuilder.success(digioResponse.getParsed(),
                    "Face match request created successfully", request));

        } catch (IllegalArgumentException ex) {
            log.warn("Bad request: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(responseBuilder.badRequest(ex.getMessage(), null, request));
        } catch (Exception ex) {
            log.error("Error processing face match request", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBuilder.serverError("Failed to process face match request",
                            ex.getMessage(), request));
        }
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            log.info("======== Webhook Received ========");

            // Extract webhook fields
            String webhookId = (String) payload.get("id");
            String eventType = (String) payload.get("event");

            // Extract nested kyc_request data
            Map<String, Object> kycRequest = null;
            Map<String, Object> kycAction = null;

            if (payload.containsKey("payload")) {
                Map<String, Object> payloadMap = (Map<String, Object>) payload.get("payload");
                if (payloadMap.containsKey("kyc_request")) {
                    kycRequest = (Map<String, Object>) payloadMap.get("kyc_request");
                }
                if (payloadMap.containsKey("kyc_action")) {
                    kycAction = (Map<String, Object>) payloadMap.get("kyc_action");
                }
            }

            String kid = kycRequest != null ? (String) kycRequest.get("id") : null;
            String status = kycRequest != null ? (String) kycRequest.get("status") : null;
            String referenceId = kycRequest != null ? (String) kycRequest.get("reference_id") : null;
            String transactionId = kycRequest != null ? (String) kycRequest.get("transaction_id") : null;

            log.info("Webhook ID: {}", webhookId);
            log.info("Event Type: {}", eventType);
            log.info("KID: {}", kid);
            log.info("Status: {}", status);
            log.info("Reference ID: {}", referenceId);

            // Set KID for audit logging interceptor
            if (kid != null) {
                request.setAttribute("auth.webhookKid", kid);
            }

            // Upsert into FM_TRANSACTIONS (business state - no duplicates)
            if (kid != null) {
                if (transactionRepository.existsByKid(kid)) {
                    // Update existing record
                    Transaction existing = transactionRepository.findByKid(kid).orElse(null);
                    if (existing != null) {
                        existing.setStatus(status);
                        existing.setReferenceId(referenceId);
                        existing.setTransactionId(transactionId);
                        transactionRepository.save(existing);
                        log.info("Updated FM_TRANSACTIONS for KID: {}", kid);
                    }
                } else {
                    // Create new record
                    Transaction newTx = Transaction.builder()
                            .id(UUID.randomUUID().toString())
                            .kid(kid)
                            .status(status)
                            .referenceId(referenceId)
                            .transactionId(transactionId)
                            .build();
                    transactionRepository.save(newTx);
                    log.info("Created new FM_TRANSACTIONS record for KID: {}", kid);
                }
            }

            log.info("====== Webhook Processing End ======");

            Map<String, String> responseData = new HashMap<>();
            responseData.put("status", "received");
            responseData.put("kid", kid != null ? kid : "N/A");

            return ResponseEntity.ok(responseBuilder.success(responseData, "Webhook received and processed", request));

        } catch (Exception ex) {
            log.error("Error processing webhook", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBuilder.serverError("Failed to process webhook", ex.getMessage(), request));
        }
    }
}
