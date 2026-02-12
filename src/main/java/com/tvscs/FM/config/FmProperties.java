package com.tvscs.FM.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "fm")
public class FmProperties {

    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank(message = "JWT secret is required")
        private String secret = "your-super-secret-key-change-in-production";

        @Positive(message = "JWT TTL minutes must be positive")
        private int ttlMinutes = 15;
    }
}
