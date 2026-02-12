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
@ConfigurationProperties(prefix = "digio")
public class DigioProperties {

    @NotBlank(message = "Digio auth token is required")
    private String authToken;

    @NotBlank(message = "Digio callback URL is required")
    private String callbackUrl = "https://tvscredit.com";

    @Positive(message = "Expire days must be a positive integer")
    private int expireDays = 90;

    @NotBlank(message = "Digio template name is required")
    private String templateName = "SELFIE COMPARE";

    @NotBlank(message = "Digio endpoint URL is required")
    private String endpoint = "https://ext.digio.in:444/client/kyc/v2/request/with_template";

    @NotBlank(message = "Digio base URL is required")
    private String baseUrl = "https://ext.digio.in/#/gateway/login";
}
