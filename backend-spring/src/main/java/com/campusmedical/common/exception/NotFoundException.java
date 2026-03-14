package com.campusmedical.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public NotFoundException(String message, Map<String, Object> details) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message, details);
    }
}
