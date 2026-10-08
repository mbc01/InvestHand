package com.investment.Group.management.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.investment.Group.management.model.Role;
import com.investment.Group.management.model.User;
import com.investment.Group.management.Repository.RoleRepository;
import com.investment.Group.management.Repository.UserRepository;

/**
 * Creates the baseline roles and an initial administrator on first startup.
 *
 * <p>The admin password comes from the {@code app.seed.admin-password}
 * property, which {@code application.properties} maps to the
 * {@code ADMIN_PASSWORD} environment variable, and is never hard-coded,
 * because a seeded credential with a known default is an immediate compromise
 * if the repository is ever public. If the variable is absent, no admin is
 * created rather than one with a guessable password.
 */
@Configuration
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";

    @Bean
    public ApplicationRunner seedData(UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${app.seed.admin-username:admin}")
                                      String adminUsername,
                                      @Value("${app.seed.admin-email:admin@example.com}")
                                      String adminEmail,
                                      @Value("${app.seed.admin-password:}")
                                      String adminPassword) {

        return args -> seed(userRepository, roleRepository, passwordEncoder,
                adminUsername, adminEmail, adminPassword);
    }

    @Transactional
    void seed(UserRepository userRepository,
              RoleRepository roleRepository,
              PasswordEncoder passwordEncoder,
              String adminUsername,
              String adminEmail,
              String adminPassword) {

        // Roles first: users reference a role name.
        Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ROLE_ADMIN)));

        roleRepository.findByName(ROLE_MEMBER)
                .orElseGet(() -> roleRepository.save(new Role(ROLE_MEMBER)));

        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user '{}' already present; skipping admin seed.", adminUsername);
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("No admin seed password configured and no admin user exists yet, "
                            + "so no admin was created. Set the ADMIN_PASSWORD environment "
                            + "variable (mapped to app.seed.admin-password in "
                            + "application.properties) and restart to seed an initial "
                            + "administrator.");
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        // Encoded immediately: the plain text is never persisted or logged.
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole.getName());
        userRepository.save(admin);

        log.info("Seeded initial admin user '{}'. Change this password before "
                + "using the system anywhere real.", adminUsername);
    }
}