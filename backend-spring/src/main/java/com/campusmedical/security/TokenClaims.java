package com.campusmedical.security;

public class TokenClaims {

    private final Long userId;
    private final String tokenType;

    public TokenClaims(Long userId, String tokenType) {
        this.userId = userId;
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenType() {
        return tokenType;
    }
}
