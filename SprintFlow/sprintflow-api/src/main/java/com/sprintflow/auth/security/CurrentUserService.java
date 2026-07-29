package com.sprintflow.auth.security;

import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResourceNotFoundException("Authenticated user was not found");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }
}
