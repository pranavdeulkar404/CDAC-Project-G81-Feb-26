package com.sprintflow.user.entity;

import com.sprintflow.common.audit.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_role_enabled", columnList = "role,account_enabled")
})
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    @Column(length = 100)
    private String otpCodeHash;

    private Instant otpExpiry;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OtpPurpose otpPurpose;

    @Column(nullable = false)
    private int otpAttempts;

    private Instant otpLastSentAt;

    @Column(nullable = false)
    private boolean otpVerified;

    @Column(nullable = false)
    private boolean accountEnabled;

    @Column(length = 100)
    private String passwordResetTokenHash;

    private Instant passwordResetTokenExpiry;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getOtpCodeHash() { return otpCodeHash; }
    public void setOtpCodeHash(String otpCodeHash) { this.otpCodeHash = otpCodeHash; }
    public Instant getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(Instant otpExpiry) { this.otpExpiry = otpExpiry; }
    public OtpPurpose getOtpPurpose() { return otpPurpose; }
    public void setOtpPurpose(OtpPurpose otpPurpose) { this.otpPurpose = otpPurpose; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }
    public Instant getOtpLastSentAt() { return otpLastSentAt; }
    public void setOtpLastSentAt(Instant otpLastSentAt) { this.otpLastSentAt = otpLastSentAt; }
    public boolean isOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }
    public boolean isAccountEnabled() { return accountEnabled; }
    public void setAccountEnabled(boolean accountEnabled) { this.accountEnabled = accountEnabled; }
    public String getPasswordResetTokenHash() { return passwordResetTokenHash; }
    public void setPasswordResetTokenHash(String passwordResetTokenHash) { this.passwordResetTokenHash = passwordResetTokenHash; }
    public Instant getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(Instant passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }
}
