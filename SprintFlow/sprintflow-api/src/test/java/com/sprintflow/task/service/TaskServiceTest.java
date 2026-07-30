package com.sprintflow.task.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.comment.repository.CommentRepository;
import com.sprintflow.integration.audit.AuditEventPublisher;
import com.sprintflow.integration.audit.AuditEventType;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.service.ProjectService;
import com.sprintflow.task.dto.TaskRequest;
import com.sprintflow.task.entity.TaskItem;
import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.task.entity.TaskStatus;
import com.sprintflow.task.repository.TaskRepository;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock CommentRepository commentRepository;
    @Mock ProjectService projectService;
    @Mock UserService userService;
    @Mock CurrentUserService currentUserService;
    @Mock NotificationService notificationService;
    @Mock AuditEventPublisher auditEventPublisher;
    TaskService service;
    User manager;
    User firstAssignee;
    User secondAssignee;
    Project project;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, commentRepository, projectService,
                userService, currentUserService, notificationService, auditEventPublisher);
        manager = user(1L, "Manager", UserRole.MANAGER);
        firstAssignee = user(2L, "First", UserRole.MEMBER);
        secondAssignee = user(3L, "Second", UserRole.MEMBER);
        project = new Project();
        project.setId(10L);
        project.setTitle("Portal");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(manager);
        when(currentUserService.requireUser()).thenReturn(manager);
        when(projectService.require(10L)).thenReturn(project);
        lenient().when(taskRepository.save(any())).thenAnswer(invocation -> {
            TaskItem item = invocation.getArgument(0);
            if (item.getId() == null) item.setId(20L);
            return item;
        });
    }

    @Test
    void createsAndNotifiesAssignedUser() {
        when(userService.require(2L)).thenReturn(firstAssignee);

        var response = service.create(request(2L));

        assertThat(response.assignedTo()).isNull();
        verify(notificationService).notify(eq(firstAssignee), eq(manager), any(), any(), any(), eq(20L), any());
        verify(auditEventPublisher).publish(eq(AuditEventType.TASK_CREATED), eq("TASK"),
                eq(20L), eq(1L), eq("Manager"), contains("Build screen"), anyMap());
    }

    @Test
    void reassignmentNotifiesOldAndNewAssignees() {
        TaskItem existing = task(firstAssignee);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userService.require(3L)).thenReturn(secondAssignee);

        service.update(20L, request(3L));

        verify(notificationService).notify(eq(firstAssignee), eq(manager), contains("reassigned from you"),
                any(), any(), eq(20L), isNull());
        verify(notificationService).notify(eq(secondAssignee), eq(manager), contains("reassigned to you"),
                any(), any(), eq(20L), any());
    }

    private TaskRequest request(Long assignee) {
        return new TaskRequest("Build screen", "Responsive project screen", TaskPriority.HIGH,
                TaskStatus.TODO, LocalDate.now().plusDays(5), 10L, assignee);
    }

    private TaskItem task(User assignee) {
        TaskItem task = new TaskItem();
        task.setId(20L);
        task.setTitle("Build screen");
        task.setDescription("Description");
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.TODO);
        task.setProject(project);
        task.setCreatedBy(manager);
        task.setAssignedTo(assignee);
        return task;
    }

    private User user(Long id, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setRole(role);
        user.setAccountEnabled(true);
        return user;
    }
}
