package com.investment.Group.management.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.investment.Group.management.model.Role;

/**
 * Spring Data JPA repository for {@link Role}.
 *
 * <p>See {@link UserRepository} for why this is a top-level interface rather
 * than a nested one inside a plain wrapper class.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}