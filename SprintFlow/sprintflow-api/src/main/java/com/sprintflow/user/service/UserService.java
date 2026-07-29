package com.sprintflow.user.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.common.exception.BusinessException;
import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.common.response.PageResponse;
import com.sprintflow.config.AdminSeeder;
import com.sprintflow.user.dto.*;
import com.sprintflow.user.entity.Profile;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.repository.ProfileRepository;
import com.sprintflow.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CurrentUserService currentUserService;

    public UserService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(String search, UserRole role, Boolean enabled, Pageable pageable) {
        return PageResponse.from(userRepository.search(clean(search), role, enabled, pageable).map(this::response));
    }

    @Transactional(readOnly = true)
    public List<UserSummary> assignableUsers() {
        return userRepository.search("", null, true, Pageable.unpaged()).stream()
                .map(this::summary)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(Long id, UserRoleRequest request) {
        User user = require(id);
        protectBootstrapAdmin(user);
        user.setRole(request.role());
        return response(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStatus(Long id, UserStatusRequest request) {
        User user = require(id);
        protectBootstrapAdmin(user);
        user.setAccountEnabled(request.enabled());
        return response(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile() {
        User user = currentUserService.requireUser();
        Profile profile = profileRepository.findByUserId(user.getId()).orElseGet(() -> emptyProfile(user));
        return profileResponse(user, profile);
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileRequest request) {
        User user = currentUserService.requireUser();
        Profile profile = profileRepository.findByUserId(user.getId()).orElseGet(() -> emptyProfile(user));
        profile.setPhone(cleanNullable(request.phone()));
        profile.setDesignation(cleanNullable(request.designation()));
        profile.setBio(cleanNullable(request.bio()));
        return profileResponse(user, profileRepository.save(profile));
    }

    public User require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserSummary summary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isAccountEnabled());
    }

    private UserResponse response(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.isAccountEnabled(), user.isOtpVerified(), user.getCreatedAt());
    }

    private Profile emptyProfile(User user) {
        Profile profile = new Profile();
        profile.setUser(user);
        return profile;
    }

    private ProfileResponse profileResponse(User user, Profile profile) {
        return new ProfileResponse(user.getId(), user.getName(), user.getEmail(), initials(user.getName()),
                profile.getPhone(), profile.getDesignation(), profile.getBio());
    }

    private String initials(String name) {
        return java.util.Arrays.stream(name.trim().split("\\s+"))
                .limit(2)
                .map(part -> part.substring(0, 1).toUpperCase())
                .reduce("", String::concat);
    }

    private void protectBootstrapAdmin(User user) {
        if (AdminSeeder.ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("The bootstrap administrator cannot be deactivated or demoted");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
