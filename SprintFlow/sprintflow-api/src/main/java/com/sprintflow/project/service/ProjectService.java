package com.sprintflow.project.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.repository.BugRepository;
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
import com.sprintflow.project.dto.ProjectRequest;
import com.sprintflow.project.dto.ProjectResponse;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.repository.ProjectRepository;
import com.sprintflow.task.repository.TaskRepository;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final BugRepository bugRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditEventPublisher auditEventPublisher;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            BugRepository bugRepository,
            CurrentUserService currentUserService,
            UserService userService,
            NotificationService notificationService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.bugRepository = bugRepository;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(
            String search, ProjectStatus status, Pageable pageable
    ) {
        User user = currentUserService.requireUser();
        boolean privileged = isPrivileged(user);
        return PageResponse.from(projectRepository
                .search(search == null ? "" : search.trim(), status, user.getId(), privileged, pageable)
                .map(this::response));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        Project project = require(id);
        requireView(project, currentUserService.requireUser());
        return response(project);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProjectResponse create(ProjectRequest request) {
        validateDates(request);
        User actor = currentUserService.requireUser();
        Project project = new Project();
        apply(project, request);
        project.setCreatedBy(actor);
        Project saved = projectRepository.save(project);
        auditEventPublisher.publish(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                saved.getId(),
                actor.getId(),
                actor.getName(),
                "Created project \"" + saved.getTitle() + "\".",
                Map.of("status", saved.getStatus().name())
        );
        return response(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProjectResponse update(Long id, ProjectRequest request) {
        validateDates(request);
        Project project = require(id);
        User actor = currentUserService.requireUser();
        requireManage(project, actor);
        ProjectStatus oldStatus = project.getStatus();
        apply(project, request);
        Project saved = projectRepository.save(project);
        if (oldStatus != saved.getStatus()) {
            notifyProjectMembers(saved, actor, oldStatus);
        }
        auditEventPublisher.publish(
                AuditEventType.PROJECT_UPDATED,
                "PROJECT",
                saved.getId(),
                actor.getId(),
                actor.getName(),
                "Updated project \"" + saved.getTitle() + "\".",
                Map.of(
                        "previousStatus", oldStatus.name(),
                        "status", saved.getStatus().name()
                )
        );
        return response(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(Long id) {
        Project project = require(id);
        User actor = currentUserService.requireUser();
        requireManage(project, actor);
        if (taskRepository.existsByProjectId(id) || bugRepository.existsByProjectId(id)) {
            throw new BusinessException("Archive projects that already contain tasks or bugs instead of deleting them");
        }
        projectRepository.delete(project);
        auditEventPublisher.publish(
                AuditEventType.PROJECT_DELETED,
                "PROJECT",
                project.getId(),
                actor.getId(),
                actor.getName(),
                "Deleted project \"" + project.getTitle() + "\".",
                Map.of("status", project.getStatus().name())
        );
    }

    public Project require(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    public void requireView(Project project, User user) {
        if (isPrivileged(user) || project.getCreatedBy().getId().equals(user.getId())
                || taskRepository.existsByProjectIdAndAssignedToId(project.getId(), user.getId())
                || bugRepository.existsByProjectIdAndAssignedToId(project.getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this project");
    }

    private void requireManage(Project project, User user) {
        if (isPrivileged(user)) {
            return;
        }
        throw new AccessDeniedException("Only a manager or administrator can manage this project");
    }

    private void notifyProjectMembers(Project project, User actor, ProjectStatus previous) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        taskRepository.findDistinctAssigneesByProjectId(project.getId())
                .forEach(user -> recipients.put(user.getId(), user));
        bugRepository.findDistinctAssigneesByProjectId(project.getId())
                .forEach(user -> recipients.put(user.getId(), user));
        String message = "Project \"" + project.getTitle() + "\" moved from "
                + previous.name().replace('_', ' ') + " to " + project.getStatus().name().replace('_', ' ') + ".";
        recipients.values().forEach(recipient -> notificationService.notify(
                recipient, actor, message, NotificationType.PROJECT_UPDATE, ReferenceType.PROJECT, project.getId(),
                new EmailNotificationRequest(recipient.getEmail(), recipient.getName(), "SprintFlow project update",
                        EmailNotificationType.PROJECT_UPDATED, message, null, project.getTitle(),
                        null, null, actor.getName(), project.getId())));
    }

    private void validateDates(ProjectRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Project end date cannot be before its start date");
        }
    }

    private void apply(Project project, ProjectRequest request) {
        project.setTitle(request.title().trim());
        project.setDescription(request.description().trim());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setStatus(request.status());
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;
    }

    private ProjectResponse response(Project project) {
        return new ProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
                project.getStartDate(), project.getEndDate(), project.getStatus(),
                userService.summary(project.getCreatedBy()),
                taskRepository.countByProjectId(project.getId()),
                bugRepository.countByProjectId(project.getId()),
                project.getCreatedAt(), project.getUpdatedAt());
    }
}
