package com.sprintflow.task.entity;

import com.sprintflow.common.audit.AuditableEntity;
import com.sprintflow.project.entity.Project;
import com.sprintflow.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_project", columnList = "project_id"),
        @Index(name = "idx_tasks_assignee_status", columnList = "assigned_to,status"),
        @Index(name = "idx_tasks_due_date", columnList = "due_date")
})
public class TaskItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", foreignKey = @ForeignKey(name = "fk_tasks_assignee"))
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_creator"))
    private User createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
