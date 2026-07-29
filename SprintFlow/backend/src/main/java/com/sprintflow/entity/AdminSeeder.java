package com.sprintflow.config;

import com.sprintflow.user.entity.Profile;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import com.sprintflow.user.repository.ProfileRepository;
import com.sprintflow.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "pranavdeulkar04@gmail.com";
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User administrator = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseGet(() -> {
            User user = new User();
            user.setName("Pranav Deulkar");
            user.setEmail(ADMIN_EMAIL);
            user.setPassword(passwordEncoder.encode("Hello@123"));
            user.setRole(UserRole.ADMIN);
            user.setAccountEnabled(true);
            user.setOtpVerified(true);
            return userRepository.save(user);
        });

        if (!profileRepository.existsByUserId(administrator.getId())) {
            Profile profile = new Profile();
            profile.setUser(administrator);
            profile.setDesignation("SprintFlow Administrator");
            profileRepository.save(profile);
        }
    }
}
