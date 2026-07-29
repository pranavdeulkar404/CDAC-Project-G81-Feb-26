package com.sprintflow.notification.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.common.response.PageResponse;
import com.sprintflow.integration.notification.EmailNotificationRequest;
import com.sprintflow.integration.notification.NotificationClient;
import com.sprintflow.notification.dto.NotificationResponse;
import com.sprintflow.notification.entity.Notification;
import com.sprintflow.notification.entity.NotificationType;
import com.sprintflow.notification.entity.ReferenceType;
import com.sprintflow.notification.repository.NotificationRepository;
import com.sprintflow.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationClient notificationClient;
    private final CurrentUserService currentUserService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationClient notificationClient,
            CurrentUserService currentUserService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationClient = notificationClient;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void notify(User recipient, User actor, String message, NotificationType type,
                       ReferenceType referenceType, Long referenceId, EmailNotificationRequest emailRequest) {
        if (recipient == null || (actor != null && recipient.getId().equals(actor.getId()))) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notificationRepository.save(notification);
        if (emailRequest != null) {
            notificationClient.send(emailRequest);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Pageable pageable) {
        User user = currentUserService.requireUser();
        return PageResponse.from(notificationRepository.findByUserId(user.getId(), pageable).map(this::response));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFalse(currentUserService.requireUser().getId());
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        User user = currentUserService.requireUser();
        Notification notification = notificationRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        return response(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(currentUserService.requireUser().getId(), Instant.now());
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.requireUser();
        Notification notification = notificationRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notification);
    }

    private NotificationResponse response(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getMessage(), notification.getType(),
                notification.getReferenceType(), notification.getReferenceId(), notification.isRead(),
                notification.getReadAt(), notification.getCreatedAt());
    }
}
