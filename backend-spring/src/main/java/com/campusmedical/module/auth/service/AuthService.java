package com.campusmedical.module.auth.service;

import com.campusmedical.common.exception.AuthenticationException;
import com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.UserRepository;
import com.campusmedical.module.auth.dto.AuthDtos;
import com.campusmedical.security.JwtTokenProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AuthenticationException("邮箱或密码错误", fieldDetail("email")));

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new AuthenticationException("邮箱或密码错误", fieldDetail("password"));
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthenticationException("该账户已被禁用，请联系管理员");
        }

        AuthDtos.LoginResponse response = new AuthDtos.LoginResponse();
        response.setAccessToken(jwtTokenProvider.generateAccessToken(user.getId()));
        response.setRefreshToken(jwtTokenProvider.generateRefreshToken(user.getId(), false));
        response.setTokenType("bearer");
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds());
        response.setUser(toUserInfo(user));
        return response;
    }

    @Transactional(readOnly = true)
    public AuthDtos.RefreshTokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AuthenticationException("刷新令牌无效或已过期");
        }

        Long userId = parseRefreshTokenUserId(refreshToken);
        String tokenType;
        try {
            tokenType = jwtTokenProvider.parse(refreshToken).getTokenType();
        } catch (Exception exception) {
            throw new AuthenticationException("刷新令牌无效或已过期");
        }

        if (!"refresh".equals(tokenType)) {
            throw new AuthenticationException("刷新令牌无效或已过期");
        }

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("用户不存在或已被禁用"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthenticationException("用户不存在或已被禁用");
        }

        AuthDtos.RefreshTokenResponse response = new AuthDtos.RefreshTokenResponse();
        response.setAccessToken(jwtTokenProvider.generateAccessToken(user.getId()));
        response.setRefreshToken(jwtTokenProvider.generateRefreshToken(user.getId(), true));
        response.setTokenType("bearer");
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds());
        return response;
    }

    @Transactional(readOnly = true)
    public Long parseRefreshTokenUserId(String refreshToken) {
        try {
            return jwtTokenProvider.parse(refreshToken).getUserId();
        } catch (Exception exception) {
            throw new AuthenticationException("刷新令牌无效或已过期");
        }
    }

    public AuthDtos.UserInfo toUserInfo(UserEntity user) {
        AuthDtos.UserInfo userInfo = new AuthDtos.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole());
        userInfo.setFullName(user.getFullName());
        userInfo.setPhone(user.getPhone());
        userInfo.setStudentId(user.getStudentId());
        userInfo.setIsActive(user.getIsActive());
        userInfo.setCreatedAt(user.getCreatedAt());
        return userInfo;
    }

    private Map<String, Object> fieldDetail(String field) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("field", field);
        return details;
    }
}
