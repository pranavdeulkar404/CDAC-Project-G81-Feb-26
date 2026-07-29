package com.sprintflow.comment.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.service.BugService;
import com.sprintflow.comment.dto.CommentRequest;
import com.sprintflow.comment.entity.WorkComment;
import com.sprintflow.comment.repository.CommentRepository;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.task.entity.TaskItem;
import com.sprintflow.task.service.TaskService;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    @Test
    void blankCommentFailsValidation() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(new CommentRequest("   "))).isNotEmpty();
    }

    @Test
    void taskCommentHasExactlyOneParentAndCreatesNotification() {
        CommentRepository repository = mock(CommentRepository.class);
        TaskService taskService = mock(TaskService.class);
        BugService bugService = mock(BugService.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        UserService users = mock(UserService.class);
        NotificationService notifications = mock(NotificationService.class);
        CommentService service =
                new CommentService(repository, taskService, bugService, currentUser, users, notifications);

        User author = user(1L);
        User assignee = user(2L);
        TaskItem task = new TaskItem();
        task.setId(9L);
        task.setTitle("Task");
        task.setCreatedBy(author);
        task.setAssignedTo(assignee);
        when(currentUser.requireUser()).thenReturn(author);
        when(taskService.require(9L)).thenReturn(task);
        when(repository.save(any())).thenAnswer(invocation -> {
            WorkComment comment = invocation.getArgument(0);
            comment.setId(5L);
            return comment;
        });

        service.addTaskComment(9L, new CommentRequest("  Ready for review  "));

        ArgumentCaptor<WorkComment> captor = ArgumentCaptor.forClass(WorkComment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTask()).isSameAs(task);
        assertThat(captor.getValue().getBug()).isNull();
        assertThat(captor.getValue().getMessage()).isEqualTo("Ready for review");
        verify(notifications).notify(eq(assignee), eq(author), any(), any(), any(), eq(9L), isNull());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(UserRole.MEMBER);
        return user;
    }
}
