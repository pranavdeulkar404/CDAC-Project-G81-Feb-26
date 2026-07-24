package com.sprintflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "profiles", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String bio;

    public Profile() {}

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
