package com.sprintflow.auth.service;

import com.sprintflow.auth.dto.*;
import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.auth.security.JwtService;
import com.sprintflow.common.exception.BusinessException;
import com.sprintflow.common.exception.ConflictException;
import com.sprintflow.common.exception.ResourceNotFoundException;
import com.sprintflow.integration.notification.EmailNotificationRequest;
import com.sprintflow.integration.notification.EmailNotificationType;
import com.sprintflow.integration.notification.NotificationClient;
import com.sprintflow.user.dto.UserSummary;
import com.sprintflow.user.entity.OtpPurpose;
import com.sprintflow.user.entity.Profile;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.repository.ProfileRepository;
import com.sprintflow.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    static final int MAX_OTP_ATTEMPTS = 5;
    static final Duration OTP_VALIDITY = Duration.ofMinutes(10);
    static final Duration OTP_RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final Duration RESET_TOKEN_VALIDITY = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final NotificationClient notificationClient;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            OtpGenerator otpGenerator,
            NotificationClient notificationClient,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpGenerator = otpGenerator;
        this.notificationClient = notificationClient;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email address already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.MEMBER);
        user.setAccountEnabled(false);
        user.setOtpVerified(false);
        String otp = issueOtp(user, OtpPurpose.ACCOUNT_VERIFICATION);
        userRepository.save(user);
        createProfileIfMissing(user);

        sendOtp(user, otp, EmailNotificationType.REGISTRATION_OTP, "Verify your SprintFlow account");
        return new MessageResponse("Registration successful. Check your email for the verification code.");
    }

    @Transactional
    public MessageResponse verifyEmail(OtpRequest request) {
        User user = requireUser(request.email());
        if (user.isOtpVerified() && user.isAccountEnabled()) {
            return new MessageResponse("Your account is already verified.");
        }
        verifyOtp(user, request.otpCode(), OtpPurpose.ACCOUNT_VERIFICATION);
        user.setOtpVerified(true);
        user.setAccountEnabled(true);
        clearOtp(user);
        userRepository.save(user);
        return new MessageResponse("Account verified successfully. You can now sign in.");
    }

    @Transactional
    public MessageResponse resendVerificationOtp(EmailRequest request) {
        User user = requireUser(request.email());
        if (user.isOtpVerified()) {
            throw new BusinessException("This account is already verified");
        }
        enforceCooldown(user);
        String otp = issueOtp(user, OtpPurpose.ACCOUNT_VERIFICATION);
        userRepository.save(user);
        sendOtp(user, otp, EmailNotificationType.REGISTRATION_OTP_RESEND, "Your new SprintFlow verification code");
        return new MessageResponse("A new verification code has been sent.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isOtpVerified()) {
            throw new BusinessException("Verify your email address before signing in");
        }
        if (!user.isAccountEnabled()) {
            throw new BusinessException("This account has been deactivated");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException exception) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtService.generate(user);
        return new AuthResponse(token, "Bearer", jwtService.expirationSeconds(), summary(user));
    }

    @Transactional
    public MessageResponse forgotPassword(EmailRequest request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.email())).ifPresent(user -> {
            if (user.isAccountEnabled() && cooldownPassed(user)) {
                String otp = issueOtp(user, OtpPurpose.PASSWORD_RESET);
                userRepository.save(user);
                sendOtp(user, otp, EmailNotificationType.PASSWORD_RESET_OTP, "Reset your SprintFlow password");
            }
        });
        return new MessageResponse("If an active account exists for that email, a reset code has been sent.");
    }

    @Transactional
    public ResetOtpResponse verifyResetOtp(OtpRequest request) {
        User user = requireUser(request.email());
        verifyOtp(user, request.otpCode(), OtpPurpose.PASSWORD_RESET);
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetTokenHash(passwordEncoder.encode(resetToken));
        user.setPasswordResetTokenExpiry(Instant.now().plus(RESET_TOKEN_VALIDITY));
        clearOtp(user);
        userRepository.save(user);
        return new ResetOtpResponse(resetToken, "Code verified. Set your new password.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }
        User user = requireUser(request.email());
        if (user.getPasswordResetTokenHash() == null
                || user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(Instant.now())
                || !passwordEncoder.matches(request.resetToken(), user.getPasswordResetTokenHash())) {
            throw new BusinessException("The password reset session is invalid or has expired");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Password changed successfully. You can now sign in.");
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }
        User user = currentUserService.requireUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new MessageResponse("Password changed successfully.");
    }

    private void verifyOtp(User user, String code, OtpPurpose expectedPurpose) {
        if (user.getOtpCodeHash() == null || user.getOtpPurpose() != expectedPurpose) {
            throw new BusinessException("No valid verification code is pending");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(Instant.now())) {
            clearOtp(user);
            userRepository.save(user);
            throw new BusinessException("The verification code has expired");
        }
        if (user.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            clearOtp(user);
            userRepository.save(user);
            throw new BusinessException("Too many incorrect attempts. Request a new code.");
        }
        if (!passwordEncoder.matches(code, user.getOtpCodeHash())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new BusinessException("The verification code is incorrect");
        }
    }

    private String issueOtp(User user, OtpPurpose purpose) {
        String otp = otpGenerator.generate();
        user.setOtpCodeHash(passwordEncoder.encode(otp));
        user.setOtpPurpose(purpose);
        user.setOtpExpiry(Instant.now().plus(OTP_VALIDITY));
        user.setOtpLastSentAt(Instant.now());
        user.setOtpAttempts(0);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiry(null);
        return otp;
    }

    private void clearOtp(User user) {
        user.setOtpCodeHash(null);
        user.setOtpPurpose(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
    }

    private void enforceCooldown(User user) {
        if (!cooldownPassed(user)) {
            long remaining = OTP_RESEND_COOLDOWN.minus(Duration.between(user.getOtpLastSentAt(), Instant.now())).toSeconds();
            throw new BusinessException("Please wait " + Math.max(1, remaining) + " seconds before requesting another code");
        }
    }

    private boolean cooldownPassed(User user) {
        return user.getOtpLastSentAt() == null
                || user.getOtpLastSentAt().plus(OTP_RESEND_COOLDOWN).isBefore(Instant.now());
    }

    private void sendOtp(User user, String otp, EmailNotificationType type, String subject) {
        notificationClient.send(new EmailNotificationRequest(
                user.getEmail(), user.getName(), subject, type, null, otp,
                null, null, null, null, null));
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private void createProfileIfMissing(User user) {
        if (!profileRepository.existsByUserId(user.getId())) {
            Profile profile = new Profile();
            profile.setUser(user);
            profileRepository.save(profile);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isAccountEnabled());
    }
}
