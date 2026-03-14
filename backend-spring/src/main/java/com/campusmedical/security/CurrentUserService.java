package com.campusmedical.security;

import com.campusmedical.common.exception.AuthenticationException;
import com.campusmedical.common.exception.AuthorizationException;
import com.campusmedical.infrastructure.persistence.mysql.entity.UserEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.UserRepository;
import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserEntity requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            throw new AuthenticationException("认证失败");
        }

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new AuthorizationException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public UserEntity requireRoles(String... roles) {
        UserEntity user = requireCurrentUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthorizationException("用户已被禁用");
        }
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(role -> role.equals(user.getRole()))) {
            throw new AuthorizationException("需要以下角色之一: " + String.join(", ", roles));
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserEntity requireAdmin() {
        return requireRoles("admin");
    }
}
