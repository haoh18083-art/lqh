package com.campusmedical.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class AuthorizationException extends AppException {

    public AuthorizationException(String message) {
        super(HttpStatus.FORBIDDEN, "AUTHORIZATION_FAILED", message);
    }

    public AuthorizationException(String message, Map<String, Object> details) {
        super(HttpStatus.FORBIDDEN, "AUTHORIZATION_FAILED", message, details);
    }
}
