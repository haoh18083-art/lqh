package com.campusmedical.common.exception;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public AppException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, Collections.<String, Object>emptyMap());
    }

    public AppException(HttpStatus status, String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details == null ? Collections.<String, Object>emptyMap() : details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
