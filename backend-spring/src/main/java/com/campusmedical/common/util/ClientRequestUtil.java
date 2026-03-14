package com.campusmedical.common.util;

import javax.servlet.http.HttpServletRequest;

public final class ClientRequestUtil {

    private ClientRequestUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            int separatorIndex = forwardedFor.indexOf(',');
            return separatorIndex >= 0 ? forwardedFor.substring(0, separatorIndex).trim() : forwardedFor;
        }

        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        return trimToNull(request.getRemoteAddr());
    }

    public static String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return trimToNull(request.getHeader("User-Agent"));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
