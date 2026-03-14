package com.campusmedical.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends AppException {

    public AuthenticationException(String message) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message);
    }

    public AuthenticationException(String message, Map<String, Object> details) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message, details);
    }
}
