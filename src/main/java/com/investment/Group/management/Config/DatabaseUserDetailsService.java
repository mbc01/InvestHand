package com.investment.Group.management.Config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.investment.Group.management.Repository.UserRepository;

/**
 * Resolves users from the {@code users} table for Spring Security.
 *
 * <p>Without this, Spring Security falls back to generating a random in-memory
 * user at startup (visible in the logs as "Using generated security password"),
 * which cannot authenticate anyone against the real database.
 *
 * <p>The stored password is a BCrypt hash; the comparison itself is performed
 * by the {@code DaoAuthenticationProvider}, never by this class.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        // Spring Security expects roles prefixed with ROLE_.
                        .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                        .build())
                // Deliberately vague: revealing whether the username exists
                // would let an attacker enumerate valid accounts.
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid username or password"));
    }
}