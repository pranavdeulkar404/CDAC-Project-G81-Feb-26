package com.sprintflow.task.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.comment.repository.CommentRepository;
import com.sprintflow.common.exception.BusinessException;
import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.common.response.PageResponse;
import com.sprintflow.integration.audit.AuditEventPublisher;
import com.sprintflow.integration.audit.AuditEventType;
import com.sprintflow.integration.notification.EmailNotificationRequest;
import com.sprintflow.integration.notification.EmailNotificationType;
import com.sprintflow.notification.entity.NotificationType;
import com.sprintflow.notification.entity.ReferenceType;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.service.ProjectService;
import com.sprintflow.task.dto.TaskRequest;
import com.sprintflow.task.dto.TaskResponse;
import com.sprintflow.task.entity.TaskItem;
import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.task.entity.TaskStatus;
import com.sprintflow.task.repository.TaskRepository;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final AuditEventPublisher auditEventPublisher;

    public TaskService(
            TaskRepository taskRepository,
            CommentRepository commentRepository,
            ProjectService projectService,
            UserService userService,
            CurrentUserService currentUserService,
            NotificationService notificationService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(
            Long projectId, Long assigneeId, TaskStatus status, TaskPriority priority,
            String search, Pageable pageable
    ) {
        User actor = currentUserService.requireUser();
        Long effectiveAssignee = actor.getRole() == UserRole.MEMBER ? actor.getId() : assigneeId;
        return PageResponse.from(taskRepository.search(projectId, effectiveAssignee, status, priority,
                search == null ? "" : search.trim(), pageable).map(this::response));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        TaskItem task = require(id);
        requireView(task, currentUserService.requireUser());
        return response(task);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public TaskResponse create(TaskRequest request) {
        User actor = currentUserService.requireUser();
        Project project = projectService.require(request.projectId());
        projectService.requireView(project, actor);
        TaskItem task = new TaskItem();
        task.setCreatedBy(actor);
        apply(task, request, project);
        TaskItem saved = taskRepository.save(task);
        if (saved.getAssignedTo() != null) {
            notifyAssignment(saved, null, saved.getAssignedTo(), actor);
        }
        auditEventPublisher.publish(
                AuditEventType.TASK_CREATED,
                "TASK",
                saved.getId(),
                actor.getId(),
                actor.getName(),
                "Created task \"" + saved.getTitle() + "\".",
                Map.of(
                        "projectId", saved.getProject().getId().toString(),
                        "priority", saved.getPriority().name(),
                        "status", saved.getStatus().name()
                )
        );
        return response(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public TaskResponse update(Long id, TaskRequest request) {
        TaskItem task = require(id);
        User actor = currentUserService.requireUser();
        requireManage(task, actor);
        User oldAssignee = task.getAssignedTo();
        TaskStatus oldStatus = task.getStatus();
        LocalDate oldDueDate = task.getDueDate();
        Project project = projectService.require(request.projectId());
        apply(task, request, project);
        TaskItem saved = taskRepository.save(task);

        if (!sameUser(oldAssignee, saved.getAssignedTo())) {
            notifyAssignment(saved, oldAssignee, saved.getAssignedTo(), actor);
        }
        if (oldStatus != saved.getStatus()) {
            notifyUpdate(saved, actor, NotificationType.STATUS_UPDATE,
                    "Task \"" + saved.getTitle() + "\" moved to " + label(saved.getStatus().name()) + ".");
        } else if (!Objects.equals(oldDueDate, saved.getDueDate())) {
            notifyUpdate(saved, actor, NotificationType.DUE_DATE_UPDATE,
                    "The due date for task \"" + saved.getTitle() + "\" is now "
                            + (saved.getDueDate() == null ? "not set" : saved.getDueDate()) + ".");
        }
        auditEventPublisher.publish(
                AuditEventType.TASK_UPDATED,
                "TASK",
                saved.getId(),
                actor.getId(),
                actor.getName(),
                "Updated task \"" + saved.getTitle() + "\".",
                Map.of(
                        "projectId", saved.getProject().getId().toString(),
                        "previousStatus", oldStatus.name(),
                        "status", saved.getStatus().name(),
                        "priority", saved.getPriority().name()
                )
        );
        return response(saved);
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        TaskItem task = require(id);
        User actor = currentUserService.requireUser();
        requireStatusUpdate(task, actor);
        TaskStatus old = task.getStatus();
        task.setStatus(status);
        TaskItem saved = taskRepository.save(task);
        if (old != status) {
            notifyUpdate(saved, actor, NotificationType.STATUS_UPDATE,
                    "Task \"" + saved.getTitle() + "\" moved from " + label(old.name())
                            + " to " + label(status.name()) + ".");
            auditEventPublisher.publish(
                    AuditEventType.TASK_STATUS_CHANGED,
                    "TASK",
                    saved.getId(),
                    actor.getId(),
                    actor.getName(),
                    "Changed task \"" + saved.getTitle() + "\" status.",
                    Map.of(
                            "projectId", saved.getProject().getId().toString(),
                            "previousStatus", old.name(),
                            "status", status.name()
                    )
            );
        }
        return response(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(Long id) {
        TaskItem task = require(id);
        User actor = currentUserService.requireUser();
        requireManage(task, actor);
        if (commentRepository.existsByTaskId(id)) {
            throw new BusinessException("Tasks with comments cannot be deleted");
        }
        taskRepository.delete(task);
        auditEventPublisher.publish(
                AuditEventType.TASK_DELETED,
                "TASK",
                task.getId(),
                actor.getId(),
                actor.getName(),
                "Deleted task \"" + task.getTitle() + "\".",
                Map.of(
                        "projectId", task.getProject().getId().toString(),
                        "status", task.getStatus().name()
                )
        );
    }

    public TaskItem require(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    public void requireView(TaskItem task, User user) {
        if (isPrivileged(user)
                || task.getCreatedBy().getId().equals(user.getId())
                || (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(user.getId()))) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this task");
    }

    private void requireManage(TaskItem task, User actor) {
        if (isPrivileged(actor)) {
            return;
        }
        throw new AccessDeniedException("Only a manager or administrator can manage this task");
    }

    private void requireStatusUpdate(TaskItem task, User actor) {
        if (isPrivileged(actor)
                || (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(actor.getId()))) {
            return;
        }
        throw new AccessDeniedException("Only the assignee or a manager can update this task status");
    }

    private void apply(TaskItem task, TaskRequest request, Project project) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setProject(project);
        task.setAssignedTo(request.assignedToId() == null ? null : activeUser(request.assignedToId()));
    }

    private User activeUser(Long id) {
        User user = userService.require(id);
        if (!user.isAccountEnabled()) {
            throw new BusinessException("Work cannot be assigned to a deactivated user");
        }
        return user;
    }

    private void notifyAssignment(TaskItem task, User oldAssignee, User newAssignee, User actor) {
        if (oldAssignee != null && !sameUser(oldAssignee, newAssignee)) {
            notificationService.notify(oldAssignee, actor,
                    "Task \"" + task.getTitle() + "\" was reassigned from you.",
                    NotificationType.REASSIGNMENT, ReferenceType.TASK, task.getId(), null);
        }
        if (newAssignee == null) {
            return;
        }
        boolean reassignment = oldAssignee != null;
        String message = "Task \"" + task.getTitle() + "\" in " + task.getProject().getTitle()
                + (reassignment ? " was reassigned to you." : " was assigned to you.");
        notificationService.notify(newAssignee, actor, message,
                reassignment ? NotificationType.REASSIGNMENT : NotificationType.ASSIGNMENT,
                ReferenceType.TASK, task.getId(),
                new EmailNotificationRequest(newAssignee.getEmail(), newAssignee.getName(),
                        reassignment ? "SprintFlow task reassigned" : "New SprintFlow task",
                        reassignment ? EmailNotificationType.TASK_REASSIGNED : EmailNotificationType.TASK_ASSIGNED,
                        message, null, task.getProject().getTitle(), task.getTitle(),
                        null, actor.getName(), task.getId()));
    }

    private void notifyUpdate(TaskItem task, User actor, NotificationType type, String message) {
        User recipient = task.getAssignedTo() != null && !task.getAssignedTo().getId().equals(actor.getId())
                ? task.getAssignedTo() : task.getCreatedBy();
        notificationService.notify(recipient, actor, message, type, ReferenceType.TASK, task.getId(),
                new EmailNotificationRequest(recipient.getEmail(), recipient.getName(), "SprintFlow task updated",
                        EmailNotificationType.TASK_UPDATED, message, null, task.getProject().getTitle(),
                        task.getTitle(), null, actor.getName(), task.getId()));
    }

    private TaskResponse response(TaskItem task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getPriority(),
                task.getStatus(), task.getDueDate(), task.getProject().getId(), task.getProject().getTitle(),
                userService.summary(task.getAssignedTo()), userService.summary(task.getCreatedBy()),
                task.getCreatedAt(), task.getUpdatedAt());
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;
    }

    private boolean sameUser(User left, User right) {
        return left == null ? right == null : right != null && left.getId().equals(right.getId());
    }

    private String label(String value) {
        return value.toLowerCase().replace('_', ' ');
    }
}
