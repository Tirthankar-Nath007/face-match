package com.tvscs.FM.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to create a canonical JSON representation of multipart form data,
 * suitable for logging/audit purposes. Stores base64_length instead of full base64 to avoid huge CLOBs.
 */
public class MultipartCanonicalizer {

    /**
     * Build a canonical JSON structure from face-match multipart request parameters.
     * 
     * @param customerName      Customer name from request
     * @param customerIdentifier Customer identifier from request
     * @param redirectFlag      "true" or "false" string indicating redirect_url preference
     * @param image             MultipartFile containing the image
     * @param base64NoPrefix    Base64-encoded image string (without prefix)
     * @return Map representing the canonical structure
     */
    public static Map<String, Object> fromFaceMatchRequest(
            String customerName,
            String customerIdentifier,
            String redirectFlag,
            MultipartFile image,
            String base64NoPrefix
    ) {
        Map<String, Object> canonical = new HashMap<>();
        canonical.put("customer_name", customerName);
        canonical.put("customer_identifier", customerIdentifier);

        // Convert redirectFlag to boolean or false if not "true"
        boolean redirect = "true".equalsIgnoreCase(redirectFlag);
        canonical.put("redirect_url", redirect);

        // Build image metadata without storing the full base64
        Map<String, Object> imageInfo = new HashMap<>();
        if (image != null) {
            imageInfo.put("filename", image.getOriginalFilename());
            imageInfo.put("contentType", image.getContentType());
            try {
                imageInfo.put("size", image.getSize());
            } catch (Exception e) {
                imageInfo.put("size", 0);
            }
            imageInfo.put("base64_length", base64NoPrefix != null ? base64NoPrefix.length() : 0);
            
            // Optional: store first 200 chars of base64 for debugging
            if (base64NoPrefix != null && base64NoPrefix.length() > 0) {
                int prefixLen = Math.min(200, base64NoPrefix.length());
                imageInfo.put("base64_prefix", base64NoPrefix.substring(0, prefixLen));
            }
        }
        canonical.put("image", imageInfo);

        return canonical;
    }
}
