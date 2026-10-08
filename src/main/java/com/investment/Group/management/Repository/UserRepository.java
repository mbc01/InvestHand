package com.investment.Group.management.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.investment.Group.management.model.User;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>Previously this was declared as a {@code public class userRepository}
 * wrapper holding a nested {@code public interface UserRepository}. That shape
 * is a trap: the outer class carries no stereotype annotation, so Spring never
 * registers the nested interface as a bean, and the "repository" is
 * consequently unusable by injection. Declaring it as a top-level interface is
 * both simpler and actually injectable.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}