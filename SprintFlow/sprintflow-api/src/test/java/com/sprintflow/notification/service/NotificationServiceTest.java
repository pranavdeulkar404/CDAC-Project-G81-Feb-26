package com.sprintflow.notification.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.integration.notification.EmailNotificationRequest;
import com.sprintflow.integration.notification.EmailNotificationType;
import com.sprintflow.integration.notification.NotificationClient;
import com.sprintflow.notification.entity.Notification;
import com.sprintflow.notification.entity.NotificationType;
import com.sprintflow.notification.entity.ReferenceType;
import com.sprintflow.notification.repository.NotificationRepository;
import com.sprintflow.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    void storesInAppNotificationEvenWhenEmailDeliveryFails() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationClient client = mock(NotificationClient.class);
        when(client.send(any())).thenReturn(false);
        NotificationService service = new NotificationService(repository, client, mock(CurrentUserService.class));
        User recipient = user(2L);
        User actor = user(1L);
        EmailNotificationRequest email = new EmailNotificationRequest(
                "member@example.com", "Member", "Assigned", EmailNotificationType.TASK_ASSIGNED,
                "Assigned", null, "Project", "Task", null, "Manager", 7L);

        assertThatCode(() -> service.notify(recipient, actor, "Assigned", NotificationType.ASSIGNMENT,
                ReferenceType.TASK, 7L, email)).doesNotThrowAnyException();

        verify(repository).save(any(Notification.class));
        verify(client).send(email);
    }

    @Test
    void doesNotNotifyActorAboutOwnAction() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationClient client = mock(NotificationClient.class);
        NotificationService service = new NotificationService(repository, client, mock(CurrentUserService.class));
        User actor = user(1L);

        service.notify(actor, actor, "Self", NotificationType.STATUS_UPDATE, ReferenceType.TASK, 7L, null);

        verifyNoInteractions(repository, client);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail("user" + id + "@example.com");
        return user;
    }
}
