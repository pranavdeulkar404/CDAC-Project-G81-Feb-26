package com.sprintflow.user.controller;

import com.sprintflow.common.response.PageResponse;
import com.sprintflow.user.dto.*;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.list(search, role, enabled,
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()));
    }

    @GetMapping("/assignable")
    List<UserSummary> assignable() {
        return userService.assignableUsers();
    }

    @PatchMapping("/{id}/role")
    UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UserRoleRequest request) {
        return userService.updateRole(id, request);
    }

    @PatchMapping("/{id}/status")
    UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return userService.updateStatus(id, request);
    }

    @GetMapping("/me/profile")
    ProfileResponse profile() {
        return userService.profile();
    }

    @PutMapping("/me/profile")
    ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        return userService.updateProfile(request);
    }
}
