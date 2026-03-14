package com.campusmedical.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final String jwtSecret;
    private final long accessTokenExpireMinutes;
    private SecretKey signingKey;

    public JwtTokenProvider(
        @Value("${app.security.jwt-secret}") String jwtSecret,
        @Value("${app.security.access-token-expire-minutes}") long accessTokenExpireMinutes
    ) {
        this.jwtSecret = jwtSecret;
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
    }

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public TokenClaims parse(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();

        Long userId = Long.valueOf(String.valueOf(claims.getSubject()));
        String tokenType = String.valueOf(claims.get("type"));
        return new TokenClaims(userId, tokenType);
    }

    public String generateAccessToken(Long userId) {
        return buildToken(userId, "access", accessTokenExpireMinutes * 60 * 1000);
    }

    public String generateRefreshToken(Long userId, boolean rememberMe) {
        long ttlMillis = rememberMe ? 7L * 24 * 60 * 60 * 1000 : accessTokenExpireMinutes * 60 * 1000;
        return buildToken(userId, "refresh", ttlMillis);
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenExpireMinutes * 60;
    }

    private String buildToken(Long userId, String tokenType, long ttlMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("type", tokenType)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }
}
