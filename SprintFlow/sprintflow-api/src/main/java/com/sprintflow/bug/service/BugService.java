package com.sprintflow.bug.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.dto.BugRequest;
import com.sprintflow.bug.dto.BugResponse;
import com.sprintflow.bug.entity.Bug;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import com.sprintflow.bug.repository.BugRepository;
import com.sprintflow.comment.repository.CommentRepository;
import com.sprintflow.common.exception.BusinessException;
import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.common.response.PageResponse;
import com.sprintflow.integration.notification.EmailNotificationRequest;
import com.sprintflow.integration.notification.EmailNotificationType;
import com.sprintflow.notification.entity.NotificationType;
import com.sprintflow.notification.entity.ReferenceType;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.service.ProjectService;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BugService {

    private final BugRepository bugRepository;
    private final CommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public BugService(
            BugRepository bugRepository,
            CommentRepository commentRepository,
            ProjectService projectService,
            UserService userService,
            CurrentUserService currentUserService,
            NotificationService notificationService
    ) {
        this.bugRepository = bugRepository;
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BugResponse> list(
            Long projectId, Long assigneeId, BugStatus status, BugSeverity severity,
            String search, Pageable pageable
    ) {
        User actor = currentUserService.requireUser();
        Long effectiveAssignee = actor.getRole() == UserRole.MEMBER ? actor.getId() : assigneeId;
        return PageResponse.from(bugRepository.search(projectId, effectiveAssignee, status, severity,
                search == null ? "" : search.trim(), pageable).map(this::response));
    }

    @Transactional(readOnly = true)
    public BugResponse get(Long id) {
        Bug bug = require(id);
        requireView(bug, currentUserService.requireUser());
        return response(bug);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BugResponse create(BugRequest request) {
        User actor = currentUserService.requireUser();
        Project project = projectService.require(request.projectId());
        projectService.requireView(project, actor);
        Bug bug = new Bug();
        bug.setReportedBy(actor);
        apply(bug, request, project);
        Bug saved = bugRepository.save(bug);
        if (saved.getAssignedTo() != null) {
            notifyAssignment(saved, null, saved.getAssignedTo(), actor);
        }
        return response(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BugResponse update(Long id, BugRequest request) {
        Bug bug = require(id);
        User actor = currentUserService.requireUser();
        requireManage(bug, actor);
        User oldAssignee = bug.getAssignedTo();
        BugStatus oldStatus = bug.getStatus();
        BugSeverity oldSeverity = bug.getSeverity();
        Project project = projectService.require(request.projectId());
        apply(bug, request, project);
        Bug saved = bugRepository.save(bug);

        if (!sameUser(oldAssignee, saved.getAssignedTo())) {
            notifyAssignment(saved, oldAssignee, saved.getAssignedTo(), actor);
        }
        if (oldStatus != saved.getStatus()) {
            notifyUpdate(saved, actor, NotificationType.STATUS_UPDATE,
                    "Bug \"" + saved.getTitle() + "\" moved to " + label(saved.getStatus().name()) + ".");
        } else if (oldSeverity != saved.getSeverity()) {
            notifyUpdate(saved, actor, NotificationType.SEVERITY_UPDATE,
                    "Bug \"" + saved.getTitle() + "\" severity changed to "
                            + label(saved.getSeverity().name()) + ".");
        }
        return response(saved);
    }

    @Transactional
    public BugResponse updateStatus(Long id, BugStatus status) {
        Bug bug = require(id);
        User actor = currentUserService.requireUser();
        requireStatusUpdate(bug, actor);
        BugStatus old = bug.getStatus();
        bug.setStatus(status);
        Bug saved = bugRepository.save(bug);
        if (old != status) {
            notifyUpdate(saved, actor, NotificationType.STATUS_UPDATE,
                    "Bug \"" + saved.getTitle() + "\" moved from " + label(old.name())
                            + " to " + label(status.name()) + ".");
        }
        return response(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(Long id) {
        Bug bug = require(id);
        requireManage(bug, currentUserService.requireUser());
        if (commentRepository.existsByBugId(id)) {
            throw new BusinessException("Bugs with comments cannot be deleted");
        }
        bugRepository.delete(bug);
    }

    public Bug require(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found"));
    }

    public void requireView(Bug bug, User user) {
        if (isPrivileged(user)
                || bug.getReportedBy().getId().equals(user.getId())
                || (bug.getAssignedTo() != null && bug.getAssignedTo().getId().equals(user.getId()))) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this bug");
    }

    private void requireManage(Bug bug, User actor) {
        if (isPrivileged(actor)) {
            return;
        }
        throw new AccessDeniedException("Only a manager or administrator can manage this bug");
    }

    private void requireStatusUpdate(Bug bug, User actor) {
        if (isPrivileged(actor)
                || (bug.getAssignedTo() != null && bug.getAssignedTo().getId().equals(actor.getId()))) {
            return;
        }
        throw new AccessDeniedException("Only the assignee or a manager can update this bug status");
    }

    private void apply(Bug bug, BugRequest request, Project project) {
        bug.setTitle(request.title().trim());
        bug.setDescription(request.description().trim());
        bug.setSeverity(request.severity());
        bug.setStatus(request.status());
        bug.setProject(project);
        bug.setAssignedTo(request.assignedToId() == null ? null : activeUser(request.assignedToId()));
    }

    private User activeUser(Long id) {
        User user = userService.require(id);
        if (!user.isAccountEnabled()) {
            throw new BusinessException("Work cannot be assigned to a deactivated user");
        }
        return user;
    }

    private void notifyAssignment(Bug bug, User oldAssignee, User newAssignee, User actor) {
        if (oldAssignee != null && !sameUser(oldAssignee, newAssignee)) {
            notificationService.notify(oldAssignee, actor,
                    "Bug \"" + bug.getTitle() + "\" was reassigned from you.",
                    NotificationType.REASSIGNMENT, ReferenceType.BUG, bug.getId(), null);
        }
        if (newAssignee == null) {
            return;
        }
        boolean reassignment = oldAssignee != null;
        String message = "Bug \"" + bug.getTitle() + "\" in " + bug.getProject().getTitle()
                + (reassignment ? " was reassigned to you." : " was assigned to you.");
        notificationService.notify(newAssignee, actor, message,
                reassignment ? NotificationType.REASSIGNMENT : NotificationType.ASSIGNMENT,
                ReferenceType.BUG, bug.getId(),
                new EmailNotificationRequest(newAssignee.getEmail(), newAssignee.getName(),
                        reassignment ? "SprintFlow bug reassigned" : "New SprintFlow bug",
                        reassignment ? EmailNotificationType.BUG_REASSIGNED : EmailNotificationType.BUG_ASSIGNED,
                        message, null, bug.getProject().getTitle(), null,
                        bug.getTitle(), actor.getName(), bug.getId()));
    }

    private void notifyUpdate(Bug bug, User actor, NotificationType type, String message) {
        User recipient = bug.getAssignedTo() != null && !bug.getAssignedTo().getId().equals(actor.getId())
                ? bug.getAssignedTo() : bug.getReportedBy();
        notificationService.notify(recipient, actor, message, type, ReferenceType.BUG, bug.getId(),
                new EmailNotificationRequest(recipient.getEmail(), recipient.getName(), "SprintFlow bug updated",
                        EmailNotificationType.BUG_UPDATED, message, null, bug.getProject().getTitle(),
                        null, bug.getTitle(), actor.getName(), bug.getId()));
    }

    private BugResponse response(Bug bug) {
        return new BugResponse(bug.getId(), bug.getTitle(), bug.getDescription(), bug.getSeverity(),
                bug.getStatus(), bug.getProject().getId(), bug.getProject().getTitle(),
                userService.summary(bug.getAssignedTo()), userService.summary(bug.getReportedBy()),
                bug.getCreatedAt(), bug.getUpdatedAt());
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
