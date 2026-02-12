package com.tvscs.FM.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvscs.FM.config.DigioProperties;
import com.tvscs.FM.exception.DigioApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DigioService {

    private final DigioProperties digioProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DigioService(DigioProperties digioProperties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.digioProperties = digioProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Result class containing both parsed Map and raw JSON string
     */
    public static class DigioResponse {
        private final Map<String, Object> parsed;
        private final String rawJson;

        public DigioResponse(Map<String, Object> parsed, String rawJson) {
            this.parsed = parsed;
            this.rawJson = rawJson;
        }

        public Map<String, Object> getParsed() {
            return parsed;
        }

        public String getRawJson() {
            return rawJson;
        }
    }

    public DigioResponse createRequest(String customerName, String customerIdentifier, String base64Image, Integer overrideExpireDays, boolean notifyCustomer) {
        int expireDays = overrideExpireDays != null ? overrideExpireDays : digioProperties.getExpireDays();

        // Validate inputs
        if (ObjectUtils.isEmpty(customerName) || ObjectUtils.isEmpty(customerIdentifier) || ObjectUtils.isEmpty(base64Image)) {
            throw new IllegalArgumentException("Customer name, customer identifier, and image are required");
        }

        // Build request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("customer_identifier", customerIdentifier);
        payload.put("customer_name", customerName);
        payload.put("reference_id", "");
        payload.put("template_name", digioProperties.getTemplateName());
        payload.put("notify_customer", notifyCustomer);
        payload.put("expire_in_days", expireDays);
        payload.put("generate_access_token", true);

        Map<String, Object> presetInput = new HashMap<>();
        Map<String, Object> image1 = new HashMap<>();
        image1.put("front_part", base64Image);
        presetInput.put("image-1", image1);
        payload.put("preset_input", presetInput);

        log.debug("Prepared Digio request payload for customer: {} with identifier: {}", customerName, customerIdentifier);

        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String authHeader = "Basic " + digioProperties.getAuthToken().trim();
        headers.set("Authorization", authHeader);

        try {
            // Convert payload to JSON string
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling Digio API endpoint: {}", digioProperties.getEndpoint());

            // Send request to Digio API
            ResponseEntity<String> response = restTemplate.exchange(
                    digioProperties.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String rawJsonResponse = response.getBody();
                Map<String, Object> responseBody = objectMapper.readValue(rawJsonResponse, new TypeReference<Map<String, Object>>() {});
                log.debug("Digio API call successful for customer: {} with response keys: {}", customerName, responseBody.keySet());
                return new DigioResponse(responseBody, rawJsonResponse);
            } else {
                log.warn("Digio API returned non-2xx status code: {}", response.getStatusCode().value());
                throw new DigioApiException(
                        "Digio API call failed with status: " + response.getStatusCode().value(),
                        response.getStatusCode().value(),
                        response.getBody()
                );
            }
        } catch (HttpStatusCodeException ex) {
            String errorBody = ex.getResponseBodyAsString();
            int statusCode = ex.getStatusCode().value();
            log.warn("Digio API returned error status: {} with body: {}", statusCode, errorBody);
            throw new DigioApiException(
                    "HTTP error when calling Digio API: " + ex.getStatusText(),
                    statusCode,
                    errorBody
            );
        } catch (Exception ex) {
            log.error("Error while calling Digio API", ex);
            throw new DigioApiException("Error while calling Digio API", ex);
        }
    }

    public static String imageToBase64NoPrefix(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
