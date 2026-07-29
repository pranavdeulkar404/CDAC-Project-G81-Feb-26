package com.sprintflow.comment.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.entity.Bug;
import com.sprintflow.bug.service.BugService;
import com.sprintflow.comment.dto.CommentRequest;
import com.sprintflow.comment.dto.CommentResponse;
import com.sprintflow.comment.entity.WorkComment;
import com.sprintflow.comment.repository.CommentRepository;
import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.notification.entity.NotificationType;
import com.sprintflow.notification.entity.ReferenceType;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.task.entity.TaskItem;
import com.sprintflow.task.service.TaskService;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final BugService bugService;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final NotificationService notificationService;

    public CommentService(
            CommentRepository commentRepository,
            TaskService taskService,
            BugService bugService,
            CurrentUserService currentUserService,
            UserService userService,
            NotificationService notificationService
    ) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.bugService = bugService;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> taskComments(Long taskId) {
        TaskItem task = taskService.require(taskId);
        taskService.requireView(task, currentUserService.requireUser());
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(this::response).toList();
    }

    @Transactional
    public CommentResponse addTaskComment(Long taskId, CommentRequest request) {
        User actor = currentUserService.requireUser();
        TaskItem task = taskService.require(taskId);
        taskService.requireView(task, actor);
        WorkComment comment = new WorkComment();
        comment.setMessage(request.message().trim());
        comment.setUser(actor);
        comment.setTask(task);
        WorkComment saved = commentRepository.save(comment);
        User recipient = chooseRecipient(task.getAssignedTo(), task.getCreatedBy(), actor);
        notificationService.notify(recipient, actor,
                actor.getName() + " commented on task \"" + task.getTitle() + "\".",
                NotificationType.COMMENT, ReferenceType.TASK, task.getId(), null);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> bugComments(Long bugId) {
        Bug bug = bugService.require(bugId);
        bugService.requireView(bug, currentUserService.requireUser());
        return commentRepository.findByBugIdOrderByCreatedAtAsc(bugId).stream().map(this::response).toList();
    }

    @Transactional
    public CommentResponse addBugComment(Long bugId, CommentRequest request) {
        User actor = currentUserService.requireUser();
        Bug bug = bugService.require(bugId);
        bugService.requireView(bug, actor);
        WorkComment comment = new WorkComment();
        comment.setMessage(request.message().trim());
        comment.setUser(actor);
        comment.setBug(bug);
        WorkComment saved = commentRepository.save(comment);
        User recipient = chooseRecipient(bug.getAssignedTo(), bug.getReportedBy(), actor);
        notificationService.notify(recipient, actor,
                actor.getName() + " commented on bug \"" + bug.getTitle() + "\".",
                NotificationType.COMMENT, ReferenceType.BUG, bug.getId(), null);
        return response(saved);
    }

    @Transactional
    public void delete(Long id) {
        User actor = currentUserService.requireUser();
        WorkComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (actor.getRole() != UserRole.ADMIN && !comment.getUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You can only delete your own comments");
        }
        commentRepository.delete(comment);
    }

    private User chooseRecipient(User primary, User secondary, User actor) {
        if (primary != null && !primary.getId().equals(actor.getId())) {
            return primary;
        }
        return secondary;
    }

    private CommentResponse response(WorkComment comment) {
        return new CommentResponse(comment.getId(), comment.getMessage(), userService.summary(comment.getUser()),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
