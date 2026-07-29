package com.sprintflow.project.service;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.repository.BugRepository;
import com.sprintflow.common.exception.BusinessException;
import com.sprintflow.notification.service.NotificationService;
import com.sprintflow.project.dto.ProjectRequest;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.repository.ProjectRepository;
import com.sprintflow.task.repository.TaskRepository;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock TaskRepository taskRepository;
    @Mock BugRepository bugRepository;
    @Mock CurrentUserService currentUserService;
    @Mock UserService userService;
    @Mock NotificationService notificationService;
    ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, taskRepository, bugRepository,
                currentUserService, userService, notificationService);
        lenient().when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsProjectWithAuthenticatedManagerAsOwner() {
        User manager = user(1L, UserRole.MANAGER);
        when(currentUserService.requireUser()).thenReturn(manager);

        var result = service.create(new ProjectRequest("Portal", "Customer portal",
                LocalDate.now(), LocalDate.now().plusMonths(1), ProjectStatus.ACTIVE));

        assertThat(result.title()).isEqualTo("Portal");
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isSameAs(manager);
    }

    @Test
    void rejectsInvalidProjectDateRange() {
        assertThatThrownBy(() -> service.create(new ProjectRequest("Portal", "Description",
                LocalDate.now(), LocalDate.now().minusDays(1), ProjectStatus.PLANNED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("end date");
    }

    @Test
    void memberCannotViewUnrelatedProject() {
        User creator = user(1L, UserRole.MANAGER);
        User member = user(2L, UserRole.MEMBER);
        Project project = project(creator);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(currentUserService.requireUser()).thenReturn(member);

        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(AccessDeniedException.class);
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(role.name());
        user.setEmail(role.name().toLowerCase() + id + "@example.com");
        user.setRole(role);
        user.setAccountEnabled(true);
        return user;
    }

    private Project project(User creator) {
        Project project = new Project();
        project.setId(7L);
        project.setTitle("Portal");
        project.setDescription("Description");
        project.setStartDate(LocalDate.now());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(creator);
        return project;
    }
}
