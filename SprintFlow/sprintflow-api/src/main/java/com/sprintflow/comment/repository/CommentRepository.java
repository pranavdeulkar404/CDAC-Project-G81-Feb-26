package com.sprintflow.comment.repository;

import com.sprintflow.comment.entity.WorkComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<WorkComment, Long> {
    List<WorkComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<WorkComment> findByBugIdOrderByCreatedAtAsc(Long bugId);
    boolean existsByTaskId(Long taskId);
    boolean existsByBugId(Long bugId);
}
