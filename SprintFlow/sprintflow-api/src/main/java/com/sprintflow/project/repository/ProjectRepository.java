package com.sprintflow.project.repository;

import com.sprintflow.project.entity.Project;
import com.sprintflow.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            select p from Project p
            where (:search = '' or lower(p.title) like lower(concat('%', :search, '%')))
              and (:status is null or p.status = :status)
              and (:privileged = true or p.createdBy.id = :userId
                   or exists (select t.id from TaskItem t where t.project.id = p.id and t.assignedTo.id = :userId)
                   or exists (select b.id from Bug b where b.project.id = p.id and b.assignedTo.id = :userId))
            """)
    Page<Project> search(
            @Param("search") String search,
            @Param("status") ProjectStatus status,
            @Param("userId") Long userId,
            @Param("privileged") boolean privileged,
            Pageable pageable
    );

    long countByStatus(ProjectStatus status);
}
