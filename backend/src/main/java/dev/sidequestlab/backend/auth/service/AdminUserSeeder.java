package dev.sidequestlab.backend.auth.service;

import dev.sidequestlab.backend.auth.config.AdminSeedProperties;
import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!test")
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminSeedProperties;

    public AdminUserSeeder(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AdminSeedProperties adminSeedProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSeedProperties = adminSeedProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        if (!StringUtils.hasText(adminSeedProperties.getEmail()) || !StringUtils.hasText(adminSeedProperties.getPassword())) {
            log.warn("Admin user was not seeded because ADMIN_EMAIL and ADMIN_PASSWORD are missing while users table is empty.");
            return;
        }

        UserEntity adminUser = new UserEntity();
        adminUser.setEmail(adminSeedProperties.getEmail().trim().toLowerCase(Locale.ROOT));
        adminUser.setPasswordHash(passwordEncoder.encode(adminSeedProperties.getPassword()));
        userRepository.save(adminUser);
    }
}
