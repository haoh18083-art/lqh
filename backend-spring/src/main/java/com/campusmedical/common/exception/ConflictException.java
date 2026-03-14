package com.campusmedical.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", message);
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", message, details);
    }
}
