package com.campusmedical.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Map<String, Object> details) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, details);
    }
}
