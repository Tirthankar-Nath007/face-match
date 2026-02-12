package com.tvscs.FM.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DigioApiException extends RuntimeException {

    private int statusCode;
    private String responseBody;

    public DigioApiException(String message) {
        super(message);
    }

    public DigioApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public DigioApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
