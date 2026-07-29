package com.sprintflow.comment.entity;

import com.sprintflow.bug.entity.Bug;
import com.sprintflow.common.audit.AuditableEntity;
import com.sprintflow.task.entity.TaskItem;
import com.sprintflow.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_task", columnList = "task_id"),
        @Index(name = "idx_comments_bug", columnList = "bug_id")
})
@Check(constraints = "(task_id is not null and bug_id is null) or (task_id is null and bug_id is not null)")
public class WorkComment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comments_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "fk_comments_task"))
    private TaskItem task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", foreignKey = @ForeignKey(name = "fk_comments_bug"))
    private Bug bug;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TaskItem getTask() { return task; }
    public void setTask(TaskItem task) { this.task = task; }
    public Bug getBug() { return bug; }
    public void setBug(Bug bug) { this.bug = bug; }
}
