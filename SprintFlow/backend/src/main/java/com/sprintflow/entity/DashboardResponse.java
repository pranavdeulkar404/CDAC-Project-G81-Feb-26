package com.sprintflow.dashboard;

import com.sprintflow.notification.dto.NotificationResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long activeProjects,
        long assignedTasks,
        long openAssignedBugs,
        long overdueTasks,
        long highPriorityBugs,
        long unreadNotifications,
        Map<String, Long> tasksByStatus,
        Map<String, Long> bugsByStatus,
        List<UpcomingWork> upcomingDueDates,
        List<NotificationResponse> recentActivity
) {
    public record UpcomingWork(Long id, String title, String projectTitle, LocalDate dueDate, String priority) {
    }
}
