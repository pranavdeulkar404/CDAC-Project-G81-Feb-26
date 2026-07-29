package com.sprintflow.user.entity;

import com.sprintflow.common.audit.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "profiles", indexes = @Index(name = "idx_profiles_user", columnList = "user_id", unique = true))
public class Profile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_profiles_user"))
    private User user;

    @Column(length = 25)
    private String phone;

    @Column(length = 100)
    private String designation;

    @Column(length = 1000)
    private String bio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
