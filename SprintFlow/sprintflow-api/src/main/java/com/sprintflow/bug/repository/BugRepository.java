package com.sprintflow.bug.repository;

import com.sprintflow.bug.entity.Bug;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sprintflow.user.entity.User;

import java.util.List;

public interface BugRepository extends JpaRepository<Bug, Long> {

    @Query("""
            select b from Bug b
            where (:projectId is null or b.project.id = :projectId)
              and (:assigneeId is null or b.assignedTo.id = :assigneeId)
              and (:status is null or b.status = :status)
              and (:severity is null or b.severity = :severity)
              and (:search = '' or lower(b.title) like lower(concat('%', :search, '%')))
            """)
    Page<Bug> search(
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId,
            @Param("status") BugStatus status,
            @Param("severity") BugSeverity severity,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByProjectId(Long projectId);
    boolean existsByProjectIdAndAssignedToId(Long projectId, Long assignedToId);
    long countByProjectId(Long projectId);
    long countByAssignedToId(Long userId);
    long countByAssignedToIdAndStatusNot(Long userId, BugStatus status);
    long countByAssignedToIdAndSeverityInAndStatusNot(Long userId, Iterable<BugSeverity> severities, BugStatus status);
    long countByAssignedToIdAndStatus(Long userId, BugStatus status);

    @Query("select distinct b.assignedTo from Bug b where b.project.id = :projectId and b.assignedTo is not null")
    List<User> findDistinctAssigneesByProjectId(@Param("projectId") Long projectId);
}
