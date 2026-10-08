package com.investment.Group.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import com.investment.Group.management.model.User;
import com.investment.Group.management.Repository.RoleRepository;
import com.investment.Group.management.Repository.UserRepository;

/**
 * Verifies that the application context starts and that the security chain and
 * repositories are wired.
 *
 * <p>Runs against in-memory H2 ({@code application-test.properties}) rather
 * than the developer's real Postgres instance, so the suite is self-contained
 * and cannot mutate real data.
 */
@SpringBootTest
@ActiveProfiles("test")
class GroupManagementApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("context loads")
    void contextLoads() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    @DisplayName("seeds the ADMIN and MEMBER roles")
    void rolesAreSeeded() {
        assertThat(roleRepository.findByName("ADMIN")).isPresent();
        assertThat(roleRepository.findByName("MEMBER")).isPresent();
    }

    @Test
    @DisplayName("stores the seeded admin password as a BCrypt hash, never in plain text")
    void adminPasswordIsHashed() {
        User admin = userRepository.findByUsername("testadmin")
                .orElseThrow(() -> new AssertionError("Expected the seeder to create 'testadmin'"));

        assertThat(admin.getPassword())
                .isNotEqualTo("TestOnly-Password-123")
                // BCrypt hashes always start with this prefix.
                .startsWith("$2");

        // And the encoder must accept the hash it produced.
        assertThat(passwordEncoder.matches("TestOnly-Password-123", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", admin.getPassword())).isFalse();
    }
}