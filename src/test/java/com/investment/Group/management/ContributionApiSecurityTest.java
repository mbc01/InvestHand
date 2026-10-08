package com.investment.Group.management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.investment.Group.management.model.User;
import com.investment.Group.management.Repository.MemberRepository;
import com.investment.Group.management.Repository.UserRepository;

/**
 * End-to-end checks over the HTTP layer, focused on the two defects that unit
 * tests cannot catch: authentication being enforced, and the correct HTTP
 * status being returned for domain failures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
/**
 * No class-level {@code @Transactional} here on purpose.
 *
 * <p>Fixture rows are written through Spring Data repositories, whose
 * {@code save} is transactional in its own right, so an enclosing test
 * transaction is not required. Adding one caused a second Spring context to be
 * built; with {@code spring.jpa.hibernate.ddl-auto=create-drop} and a shared
 * in-memory H2 URL, that second context dropped the {@code contributions}
 * table out from under the first and the suite failed with "Table
 * CONTRIBUTIONS not found".
 *
 * <p>Each test therefore uses a distinct username, so committed fixture rows
 * cannot collide on the unique username constraint.
 */
class ContributionApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void createUser(String username, String role) {
        User user = new User(username, username + "@example.com",
                passwordEncoder.encode("Passw0rd-" + role), role);
        userRepository.save(user);
    }

    /**
     * Persists a member for {@code username} and returns its generated id.
     *
     * <p>Uses the repository rather than a raw {@code EntityManager}: the
     * repository call runs in a transaction of its own and returns the saved
     * entity with its id populated. Calling {@code entityManager.persist}
     * outside a transaction threw {@code TransactionRequiredException}.
     */
    private Long createMember(String username, String fullName) {
        User user = userRepository.findByUsername(username).orElseThrow();
        var member = new com.investment.Group.management.model.Member(
                user, fullName, new java.math.BigDecimal("10.00"), "ACTIVE");
        return memberRepository.save(member).getId();
    }

    @Test
    @DisplayName("rejects an unauthenticated read with 401")
    void anonymousReadIsRejected() throws Exception {
        mockMvc.perform(get("/api/contributions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects an unauthenticated write with 401")
    void anonymousWriteIsRejected() throws Exception {
        // This request previously succeeded with 200 and no credentials.
        mockMvc.perform(post("/api/contributions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"month\":\"JANUARY\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects bad credentials with 401")
    void wrongPasswordIsRejected() throws Exception {
        createUser("alice", "ADMIN");

        mockMvc.perform(post("/api/contributions/1")
                        .with(httpBasic("alice", "not-the-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"month\":\"JANUARY\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects a write from a MEMBER with 403")
    void nonAdminWriteIsForbidden() throws Exception {
        createUser("plainMember", "MEMBER");

        // Authorization is enforced by role on the controller, so a valid
        // signed-in non-admin is refused even with correct credentials. This
        // replaced an earlier CSRF-token assertion, which no longer means
        // anything now that CSRF is disabled for this stateless API.
        mockMvc.perform(post("/api/contributions/1")
                        .with(httpBasic("plainMember", "Passw0rd-MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"month\":\"JANUARY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("keeps actuator health reachable without credentials")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returns 404 for an unknown member, not 500")
    void unknownMemberReturnsNotFound() throws Exception {
        createUser("admin404", "ADMIN");

        mockMvc.perform(post("/api/contributions/424242")
                        .with(httpBasic("admin404", "Passw0rd-ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"month\":\"JANUARY\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("rejects a non-positive amount with 400")
    void invalidAmountReturnsBadRequest() throws Exception {
        createUser("admin400", "ADMIN");

        mockMvc.perform(post("/api/contributions/1")
                        .with(httpBasic("admin400", "Passw0rd-ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-5,\"month\":\"JANUARY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects a non-numeric id with 400 rather than 500")
    void nonNumericIdReturnsBadRequest() throws Exception {
        createUser("adminBadId", "ADMIN");

        mockMvc.perform(post("/api/contributions/not-a-number")
                        .with(httpBasic("adminBadId", "Passw0rd-ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"month\":\"JANUARY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("returns 409 for a duplicate monthly contribution, not 500")
    void duplicateContributionReturnsConflict() throws Exception {
        createUser("admin409", "ADMIN");

        String body = "{\"amount\":500,\"month\":\"JANUARY\"}";

        // The first insert needs a real member to exist.
        Long memberId = createMember("admin409", "Test");

        mockMvc.perform(post("/api/contributions/" + memberId)
                        .with(httpBasic("admin409", "Passw0rd-ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                // The status is PAID on or before the 5th and LATE afterwards,
                // so which one applies depends on the day the suite runs.
                .andExpect(jsonPath("$.status").value(
                        org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.is("PAID"),
                                org.hamcrest.Matchers.is("LATE"))));

        mockMvc.perform(post("/api/contributions/" + memberId)
                        .with(httpBasic("admin409", "Passw0rd-ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("never echoes the password back in a response body")
    void responseBodyNeverContainsPassword() throws Exception {
        createUser("adminLeak", "ADMIN");

        Long memberId = createMember("adminLeak", "LeakCheck");

        String response = mockMvc.perform(post("/api/contributions/" + memberId)
                        .with(httpBasic("adminLeak", "Passw0rd-ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":250,\"month\":\"MAY\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // The BCrypt hash itself must not appear either.
        org.assertj.core.api.Assertions.assertThat(response)
                .doesNotContain("$2")
                .doesNotContain("password")
                .doesNotContain("Passw0rd");
    }
}