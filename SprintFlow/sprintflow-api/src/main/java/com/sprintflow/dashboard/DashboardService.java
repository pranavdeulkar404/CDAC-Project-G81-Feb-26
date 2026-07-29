package com.sprintflow.dashboard;

import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import com.sprintflow.bug.repository.BugRepository;
import com.sprintflow.notification.dto.NotificationResponse;
import com.sprintflow.notification.entity.Notification;
import com.sprintflow.notification.repository.NotificationRepository;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.repository.ProjectRepository;
import com.sprintflow.task.entity.TaskStatus;
import com.sprintflow.task.repository.TaskRepository;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final BugRepository bugRepository;
    private final NotificationRepository notificationRepository;

    public DashboardService(
            CurrentUserService currentUserService,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            BugRepository bugRepository,
            NotificationRepository notificationRepository
    ) {
        this.currentUserService = currentUserService;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.bugRepository = bugRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        User user = currentUserService.requireUser();
        boolean privileged = user.getRole() != UserRole.MEMBER;
        long activeProjects = projectRepository
                .search("", ProjectStatus.ACTIVE, user.getId(), privileged, PageRequest.of(0, 1))
                .getTotalElements();

        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status.name(), taskRepository.countByAssignedToIdAndStatus(user.getId(), status));
        }
        Map<String, Long> bugsByStatus = new LinkedHashMap<>();
        for (BugStatus status : BugStatus.values()) {
            bugsByStatus.put(status.name(), bugRepository.countByAssignedToIdAndStatus(user.getId(), status));
        }

        List<DashboardResponse.UpcomingWork> upcoming = taskRepository
                .findTop5ByAssignedToIdAndDueDateGreaterThanEqualAndStatusNotOrderByDueDateAsc(
                        user.getId(), LocalDate.now(), TaskStatus.COMPLETED)
                .stream()
                .map(task -> new DashboardResponse.UpcomingWork(
                        task.getId(), task.getTitle(), task.getProject().getTitle(),
                        task.getDueDate(), task.getPriority().name()))
                .toList();

        List<NotificationResponse> recent = notificationRepository
                .findByUserId(user.getId(), PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                .map(this::notification)
                .getContent();

        return new DashboardResponse(
                activeProjects,
                taskRepository.countByAssignedToId(user.getId()),
                bugRepository.countByAssignedToIdAndStatusNot(user.getId(), BugStatus.CLOSED),
                taskRepository.countByAssignedToIdAndDueDateBeforeAndStatusNot(
                        user.getId(), LocalDate.now(), TaskStatus.COMPLETED),
                bugRepository.countByAssignedToIdAndSeverityInAndStatusNot(
                        user.getId(), Arrays.asList(BugSeverity.HIGH, BugSeverity.CRITICAL), BugStatus.CLOSED),
                notificationRepository.countByUserIdAndReadFalse(user.getId()),
                tasksByStatus,
                bugsByStatus,
                upcoming,
                recent
        );
    }

    private NotificationResponse notification(Notification item) {
        return new NotificationResponse(item.getId(), item.getMessage(), item.getType(),
                item.getReferenceType(), item.getReferenceId(), item.isRead(),
                item.getReadAt(), item.getCreatedAt());
    }
}
