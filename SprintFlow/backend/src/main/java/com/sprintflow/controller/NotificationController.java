package com.sprintflow.controller;

import com.sprintflow.entity.Notification;
import com.sprintflow.entity.User;
import com.sprintflow.repository.NotificationRepository;
import com.sprintflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<NotificationResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public NotificationResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@Valid @RequestBody NotificationRequest request) {
        Notification notification = new Notification();
        apply(notification, request);
        return toResponse(repository.save(notification));
    }

    @PutMapping("/{id}")
    public NotificationResponse update(@PathVariable Long id, @Valid @RequestBody NotificationRequest request) {
        Notification notification = find(id);
        apply(notification, request);
        return toResponse(repository.save(notification));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Notification find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void apply(Notification notification, NotificationRequest request) {
        notification.setMessage(request.message());
        notification.setType(request.type());
        notification.setCreatedAt(request.createdAt() == null ? LocalDateTime.now() : request.createdAt());
        notification.setUser(findUser(request.userId()));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getMessage(), notification.getType(),
                notification.getCreatedAt(), notification.getUser().getId(), notification.getUser().getName());
    }

    public record NotificationRequest(
            @NotBlank String message, @NotBlank String type,
            LocalDateTime createdAt, @NotNull Long userId) {}

    public record NotificationResponse(
            Long id, String message, String type, LocalDateTime createdAt,
            Long userId, String userName) {}
}
