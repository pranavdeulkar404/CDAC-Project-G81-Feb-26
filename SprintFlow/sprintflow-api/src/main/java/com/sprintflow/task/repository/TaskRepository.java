package com.sprintflow.task.repository;

import com.sprintflow.task.entity.TaskItem;
import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import com.sprintflow.user.entity.User;

public interface TaskRepository extends JpaRepository<TaskItem, Long> {

    @Query("""
            select t from TaskItem t
            where (:projectId is null or t.project.id = :projectId)
              and (:assigneeId is null or t.assignedTo.id = :assigneeId)
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:search = '' or lower(t.title) like lower(concat('%', :search, '%')))
            """)
    Page<TaskItem> search(
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByProjectId(Long projectId);
    boolean existsByProjectIdAndAssignedToId(Long projectId, Long assignedToId);
    long countByProjectId(Long projectId);
    long countByAssignedToId(Long userId);
    long countByAssignedToIdAndStatusNot(Long userId, TaskStatus status);
    long countByAssignedToIdAndDueDateBeforeAndStatusNot(Long userId, LocalDate dueDate, TaskStatus status);
    long countByAssignedToIdAndStatus(Long userId, TaskStatus status);

    List<TaskItem> findTop5ByAssignedToIdAndDueDateGreaterThanEqualAndStatusNotOrderByDueDateAsc(
            Long userId, LocalDate date, TaskStatus status);

    @Query("select distinct t.assignedTo from TaskItem t where t.project.id = :projectId and t.assignedTo is not null")
    List<User> findDistinctAssigneesByProjectId(@Param("projectId") Long projectId);
}
