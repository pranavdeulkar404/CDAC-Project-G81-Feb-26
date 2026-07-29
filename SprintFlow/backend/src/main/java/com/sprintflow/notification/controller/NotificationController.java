package com.sprintflow.notification.controller;

import com.sprintflow.auth.dto.MessageResponse;
import com.sprintflow.common.response.PageResponse;
import com.sprintflow.notification.dto.NotificationResponse;
import com.sprintflow.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    PageResponse<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return notificationService.list(
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()));
    }

    @GetMapping("/unread-count")
    Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount());
    }

    @PatchMapping("/{id}/read")
    NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PatchMapping("/read-all")
    Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        notificationService.delete(id);
    }
}
