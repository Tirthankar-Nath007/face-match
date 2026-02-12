package com.tvscs.FM.utils;

import com.tvscs.FM.config.DigioProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class DigioUrlBuilder {

    private final DigioProperties digioProperties;

    public DigioUrlBuilder(DigioProperties digioProperties) {
        this.digioProperties = digioProperties;
    }

    @SuppressWarnings("unchecked")
    public String build(Map<String, Object> response, String redirectUrl) {
        String kidId = (String) response.get("id");
        String signerIdentifier = (String) response.get("customer_identifier");
        String txnId = (String) response.getOrDefault("transaction_id", "RANDOM-TXN-" + System.currentTimeMillis());
        Map<String, Object> accessToken = (Map<String, Object>) response.get("access_token");
        String tokenId = accessToken != null ? (String) accessToken.get("id") : null;

        if (kidId == null || signerIdentifier == null || tokenId == null) {
            throw new IllegalArgumentException("Missing required fields: id, customer_identifier, or access_token.id");
        }

        String encodedRedirect = URLEncoder.encode(redirectUrl == null ? "" : redirectUrl, StandardCharsets.UTF_8);
        String url = String.format("%s/%s/%s/%s?token_id=%s&redirect_url=%s",
                digioProperties.getBaseUrl(),
                kidId,
                txnId,
                signerIdentifier,
                tokenId,
                encodedRedirect);

        log.debug("Generated Digio redirect URL: {}", url);
        return url;
    }
}
