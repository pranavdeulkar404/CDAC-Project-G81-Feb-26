package com.sprintflow.bug.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.dto.BugRequest;
import com.sprintflow.bug.entity.Bug;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import com.sprintflow.bug.repository.BugRepository;
import com.sprintflow.comment.repository.CommentRepository;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.service.ProjectService;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BugServiceTest {

    @Mock BugRepository bugRepository;
    @Mock CommentRepository commentRepository;
    @Mock ProjectService projectService;
    @Mock UserService userService;
    @Mock CurrentUserService currentUserService;
    @Mock NotificationService notificationService;
    BugService service;
    User manager;
    User firstAssignee;
    User secondAssignee;
    Project project;

    @BeforeEach
    void setUp() {
        service = new BugService(bugRepository, commentRepository, projectService,
                userService, currentUserService, notificationService);
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
        lenient().when(bugRepository.save(any())).thenAnswer(invocation -> {
            Bug item = invocation.getArgument(0);
            if (item.getId() == null) item.setId(30L);
            return item;
        });
    }

    @Test
    void createsAssignedBugAndSendsNotification() {
        when(userService.require(2L)).thenReturn(firstAssignee);
        service.create(request(2L));
        verify(notificationService).notify(eq(firstAssignee), eq(manager), contains("assigned to you"),
                any(), any(), eq(30L), any());
    }

    @Test
    void bugCanBeReassigned() {
        Bug existing = bug(firstAssignee);
        when(bugRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(userService.require(3L)).thenReturn(secondAssignee);

        service.update(30L, request(3L));

        verify(notificationService).notify(eq(firstAssignee), eq(manager), contains("reassigned from you"),
                any(), any(), eq(30L), isNull());
        verify(notificationService).notify(eq(secondAssignee), eq(manager), contains("reassigned to you"),
                any(), any(), eq(30L), any());
    }

    private BugRequest request(Long assignee) {
        return new BugRequest("Login issue", "Login button fails", BugSeverity.CRITICAL,
                BugStatus.OPEN, 10L, assignee);
    }

    private Bug bug(User assignee) {
        Bug bug = new Bug();
        bug.setId(30L);
        bug.setTitle("Login issue");
        bug.setDescription("Login button fails");
        bug.setSeverity(BugSeverity.CRITICAL);
        bug.setStatus(BugStatus.OPEN);
        bug.setProject(project);
        bug.setReportedBy(manager);
        bug.setAssignedTo(assignee);
        return bug;
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
