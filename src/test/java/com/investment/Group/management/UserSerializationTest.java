package com.investment.Group.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.investment.Group.management.dto.ContributionResponse;
import com.investment.Group.management.model.Member;
import com.investment.Group.management.model.User;

/**
 * Regression test for the credential leak.
 *
 * <p>{@code User.getPassword()} used to be an ordinary getter, so Jackson
 * serialised the stored password hash into every JSON response that happened
 * to include a user. Verified here so the leak cannot be reintroduced by a
 * later refactor.
 */
class UserSerializationTest {

    /**
     * Mirrors the mapper Spring Boot auto-configures for the running
     * application: {@code JavaTimeModule} registered and timestamps written
     * as ISO strings.
     *
     * <p>A bare {@code new ObjectMapper()} has no JavaTimeModule, so it cannot
     * serialise {@code java.time} fields at all. Serialising entities by hand
     * therefore has to use the same mapper the HTTP layer uses, otherwise this
     * test would assert against a configuration the application never runs and
     * would fail on {@code User.createdAt} / {@code Contribution.paymentDate}
     * before reaching the assertion it exists to make.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("never serialises the password field")
    void passwordIsNotSerialised() throws Exception {
        User user = new User("alice", "alice@example.com", "PLAINTEXT-SECRET", "ADMIN");

        String json = objectMapper.writeValueAsString(user);

        assertThat(json).doesNotContain("PLAINTEXT-SECRET");
        assertThat(json).doesNotContain("\"password\"");
        // Sanity check that the non-sensitive fields are still present.
        assertThat(json).contains("alice");
        assertThat(json).contains("alice@example.com");
    }

    @Test
    @DisplayName("does not leak a password nested inside a Member either")
    void passwordIsNotSerialisedWhenNested() throws Exception {
        User user = new User("bob", "bob@example.com", "NESTED-SECRET", "MEMBER");
        Member member = new Member(user, "Bob", new java.math.BigDecimal("25.00"), "ACTIVE");

        String json = objectMapper.writeValueAsString(member);

        assertThat(json).doesNotContain("NESTED-SECRET");
        assertThat(json).doesNotContain("\"password\"");
    }

    @Test
    @DisplayName("contribution response exposes only the intended fields")
    void contributionResponseIsExplicit() throws Exception {
        User user = new User("carol", "carol@example.com", "DTO-SECRET", "MEMBER");
        Member member = new Member(user, "Carol", new java.math.BigDecimal("50.00"), "ACTIVE");

        var contribution = new com.investment.Group.management.model.Contribution(
                new java.math.BigDecimal("1234.56"), java.time.LocalDate.of(2026, 3, 2),
                "MARCH", "PAID", member);

        String json = objectMapper.writeValueAsString(ContributionResponse.from(contribution));

        assertThat(json).doesNotContain("DTO-SECRET");
        assertThat(json).contains("1234.56");
        assertThat(json).contains("MARCH");
        // Only the member id is exposed, not the whole user object graph.
        assertThat(json).doesNotContain("username");
        assertThat(json).doesNotContain("email");
    }
}