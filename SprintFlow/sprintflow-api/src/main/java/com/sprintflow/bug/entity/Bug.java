package com.sprintflow.bug.entity;

import com.sprintflow.common.audit.AuditableEntity;
import com.sprintflow.project.entity.Project;
import com.sprintflow.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "bugs", indexes = {
        @Index(name = "idx_bugs_project", columnList = "project_id"),
        @Index(name = "idx_bugs_assignee_status", columnList = "assigned_to,status"),
        @Index(name = "idx_bugs_severity", columnList = "severity")
})
public class Bug extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BugSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BugStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bugs_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", foreignKey = @ForeignKey(name = "fk_bugs_assignee"))
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false, foreignKey = @ForeignKey(name = "fk_bugs_reporter"))
    private User reportedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BugSeverity getSeverity() { return severity; }
    public void setSeverity(BugSeverity severity) { this.severity = severity; }
    public BugStatus getStatus() { return status; }
    public void setStatus(BugStatus status) { this.status = status; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }
}
