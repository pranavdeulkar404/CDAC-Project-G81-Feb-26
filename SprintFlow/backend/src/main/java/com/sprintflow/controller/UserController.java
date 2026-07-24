package com.sprintflow.controller;

import com.sprintflow.entity.User;
import com.sprintflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        User user = new User();
        apply(user, request);
        return toResponse(repository.save(user));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        User user = find(id);
        apply(user, request);
        return toResponse(repository.save(user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private User find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void apply(User user, UserRequest request) {
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(request.role());
        user.setOtpCode(blankToNull(request.otpCode()));
        user.setOtpExpiry(request.otpExpiry());
        user.setOtpVerified(Boolean.TRUE.equals(request.otpVerified()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPassword(),
                user.getRole(), user.getOtpCode(), user.getOtpExpiry(), user.getOtpVerified());
    }

    public record UserRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String role,
            String otpCode,
            LocalDateTime otpExpiry,
            Boolean otpVerified) {}

    public record UserResponse(
            Long id, String name, String email, String password, String role,
            String otpCode, LocalDateTime otpExpiry, Boolean otpVerified) {}
}
