package com.campusmedical.common.exception;

import org.springframework.http.HttpStatus;

public class SettingsEncryptionException extends AppException {

    public SettingsEncryptionException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "SETTINGS_ENCRYPTION_ERROR", message);
    }
}
