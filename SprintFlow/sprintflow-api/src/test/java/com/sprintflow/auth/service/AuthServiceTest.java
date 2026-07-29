package com.sprintflow.auth.service;

import com.sprintflow.auth.dto.*;
import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.auth.security.JwtService;
import com.sprintflow.common.exception.BusinessException;
import com.sprintflow.common.exception.ConflictException;
import com.sprintflow.integration.notification.NotificationClient;
import com.sprintflow.user.entity.OtpPurpose;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.repository.ProfileRepository;
import com.sprintflow.user.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProfileRepository profileRepository;
    @Mock OtpGenerator otpGenerator;
    @Mock NotificationClient notificationClient;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock CurrentUserService currentUserService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, profileRepository, passwordEncoder, otpGenerator,
                notificationClient, authenticationManager, jwtService, currentUserService);
        lenient().when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(otpGenerator.generate()).thenReturn("123456");
    }

    @Test
    void registrationValidationRejectsWeakPasswordAndInvalidEmail() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        RegisterRequest request = new RegisterRequest("A", "not-an-email", "weak");

        assertThat(validator.validate(request)).extracting("propertyPath").hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void registrationHashesPasswordGeneratesOtpAndCreatesDisabledMember() {
        when(userRepository.existsByEmailIgnoreCase("member@example.com")).thenReturn(false);
        when(profileRepository.existsByUserId(any())).thenReturn(false);
        when(notificationClient.send(any())).thenReturn(true);

        service.register(new RegisterRequest("  Member Name  ", " MEMBER@EXAMPLE.COM ", "Member@123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("member@example.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(saved.isAccountEnabled()).isFalse();
        assertThat(saved.isOtpVerified()).isFalse();
        assertThat(passwordEncoder.matches("Member@123", saved.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("123456", saved.getOtpCodeHash())).isTrue();
        assertThat(saved.getOtpPurpose()).isEqualTo(OtpPurpose.ACCOUNT_VERIFICATION);
        verify(notificationClient).send(any());
    }

    @Test
    void duplicateEmailIsPrevented() {
        when(userRepository.existsByEmailIgnoreCase("member@example.com")).thenReturn(true);
        assertThatThrownBy(() ->
                service.register(new RegisterRequest("Member", "member@example.com", "Member@123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void emailDeliveryFailureDoesNotUndoRegistration() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(profileRepository.existsByUserId(any())).thenReturn(false);
        when(notificationClient.send(any())).thenReturn(false);

        MessageResponse result =
                service.register(new RegisterRequest("Member", "member@example.com", "Member@123"));

        assertThat(result.message()).contains("Registration successful");
        verify(userRepository).save(any());
    }

    @Test
    void verifiesCorrectOtpAndEnablesAccount() {
        User user = pendingUser("123456", Instant.now().plusSeconds(300), 0);
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));

        service.verifyEmail(new OtpRequest("member@example.com", "123456"));

        assertThat(user.isOtpVerified()).isTrue();
        assertThat(user.isAccountEnabled()).isTrue();
        assertThat(user.getOtpCodeHash()).isNull();
    }

    @Test
    void rejectsExpiredOtp() {
        User user = pendingUser("123456", Instant.now().minusSeconds(1), 0);
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyEmail(new OtpRequest("member@example.com", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void incorrectOtpIncrementsAttemptsAndAttemptLimitInvalidatesIt() {
        User user = pendingUser("123456", Instant.now().plusSeconds(300), 4);
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyEmail(new OtpRequest("member@example.com", "999999")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incorrect");
        assertThat(user.getOtpAttempts()).isEqualTo(5);

        assertThatThrownBy(() -> service.verifyEmail(new OtpRequest("member@example.com", "999999")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Too many");
        assertThat(user.getOtpCodeHash()).isNull();
    }

    @Test
    void loginSucceedsAndReturnsJwtForVerifiedUser() {
        User user = activeUser(UserRole.MEMBER);
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("signed.jwt");
        when(jwtService.expirationSeconds()).thenReturn(28800L);

        AuthResponse response = service.login(new LoginRequest("member@example.com", "Member@123"));

        assertThat(response.token()).isEqualTo("signed.jwt");
        assertThat(response.user().email()).isEqualTo("member@example.com");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginRejectsBadPasswordAndUnverifiedUser() {
        User active = activeUser(UserRole.MEMBER);
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(active));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThatThrownBy(() -> service.login(new LoginRequest("member@example.com", "Wrong@123")))
                .isInstanceOf(BadCredentialsException.class);

        User pending = pendingUser("123456", Instant.now().plusSeconds(300), 0);
        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.login(new LoginRequest("pending@example.com", "Member@123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Verify");
    }

    @Test
    void preconfiguredAdministratorCanLoginWithoutOtpFlow() {
        User admin = activeUser(UserRole.ADMIN);
        admin.setEmail("pranavdeulkar04@gmail.com");
        when(userRepository.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        when(jwtService.generate(admin)).thenReturn("admin.jwt");

        AuthResponse response = service.login(new LoginRequest(admin.getEmail(), "Hello@123"));

        assertThat(response.user().role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.token()).isEqualTo("admin.jwt");
    }

    @Test
    void forgotPasswordIsGenericAndResetTokenCannotBeReused() {
        assertThat(service.forgotPassword(new EmailRequest("unknown@example.com")).message())
                .startsWith("If an active account");

        User user = activeUser(UserRole.MEMBER);
        user.setOtpCodeHash(passwordEncoder.encode("123456"));
        user.setOtpPurpose(OtpPurpose.PASSWORD_RESET);
        user.setOtpExpiry(Instant.now().plusSeconds(300));
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));

        ResetOtpResponse verified = service.verifyResetOtp(new OtpRequest("member@example.com", "123456"));
        assertThat(verified.resetToken()).isNotBlank();

        service.resetPassword(new ResetPasswordRequest(
                "member@example.com", verified.resetToken(), "Changed@123", "Changed@123"));
        assertThat(passwordEncoder.matches("Changed@123", user.getPassword())).isTrue();
        assertThat(user.getPasswordResetTokenHash()).isNull();

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(
                "member@example.com", verified.resetToken(), "Again@123", "Again@123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid");
    }

    private User pendingUser(String otp, Instant expiry, int attempts) {
        User user = new User();
        user.setId(1L);
        user.setName("Member");
        user.setEmail(attempts == 0 ? "member@example.com" : "member@example.com");
        user.setPassword(passwordEncoder.encode("Member@123"));
        user.setRole(UserRole.MEMBER);
        user.setAccountEnabled(false);
        user.setOtpVerified(false);
        user.setOtpCodeHash(passwordEncoder.encode(otp));
        user.setOtpPurpose(OtpPurpose.ACCOUNT_VERIFICATION);
        user.setOtpExpiry(expiry);
        user.setOtpAttempts(attempts);
        return user;
    }

    private User activeUser(UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setName("Member");
        user.setEmail("member@example.com");
        user.setPassword(passwordEncoder.encode("Member@123"));
        user.setRole(role);
        user.setAccountEnabled(true);
        user.setOtpVerified(true);
        return user;
    }
}
