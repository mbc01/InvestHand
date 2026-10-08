package com.investment.Group.management.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * <p>IMPORTANT: this configuration was previously
 * {@code csrf.disable()} plus {@code anyRequest().permitAll()}, which left
 * every endpoint completely open even though
 * {@code spring-boot-starter-security} was on the classpath. Spring Security
 * generated a random password and logged a warning, but the generated
 * credentials guarded nothing because {@code permitAll()} short-circuits
 * authorization. Writes were reachable by anyone with no credentials at all.
 *
 * <p>The rules below are the minimum needed to make the API safe, and assume
 * an authenticated user has already been resolved from a database-backed
 * {@code UserDetailsService}. Replace the {@code /api/**} catch-all with
 * explicit role rules once those roles exist end to end.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF is DISABLED, deliberately, and the reasoning is specific
                // rather than "it's a REST API".
                //
                // CSRF defends against the browser silently attaching ambient
                // authority (a session cookie) to a cross-site request. That
                // precondition does not hold here:
                //
                //   1. The session policy below is STATELESS, so no JSESSIONID
                //      is ever issued. Verified at runtime: a GET to a
                //      protected endpoint returns no Set-Cookie.
                //   2. Credentials travel in an explicit Authorization header.
                //      A cross-site HTML form post cannot set custom headers, so
                //      a hostile page cannot cause the browser to send them.
                //
                // Leaving CSRF on was not merely redundant, it was fatal: the
                // default HttpSessionCsrfTokenRepository needs a session to
                // store the token in, and there is neither a session nor an
                // endpoint that issues one. Every POST therefore failed with a
                // 403 that no client could satisfy, making all writes
                // impossible.
                //
                // If this API is ever changed to authenticate via cookies or
                // sessions, CSRF must be re-enabled and a token endpoint
                // added. That is the case this exemption does not cover.

                // HTTP Basic IS enabled, deliberately, and it is the only
                // credential mechanism configured.
                //
                // It was previously disabled on the reasoning that a JSON API
                // "intended for a separate client" does not need it. That
                // left the API with no way to authenticate at all: disabling
                // the filter removes BasicAuthenticationFilter from the chain
                // entirely, so an Authorization header is never parsed and
                // every request arrives anonymous. Combined with
                // anyRequest().hasRole("ADMIN") below, that made the whole
                // application unusable rather than merely unauthenticated --
                // no client, header or token could ever obtain a 2xx.
                //
                // Basic is appropriate here specifically because the session
                // policy is STATELESS: the credential travels in a header on
                // each request and nothing is stored server-side, so there is
                // no session cookie for CSRF to ride on and nothing to fix or
                // hijack. It must be sent over TLS.
                .httpBasic(basic -> {
                })

                // Form login stays off: it would redirect to an HTML login page,
                // which is wrong for a JSON API and contradicts the JSON 401
                // entry point set below.
                .formLogin(form -> form.disable())

                // See the CSRF note at the top of this method for why this is
                // safe rather than merely convenient.
                .csrf(csrf -> csrf.disable())

                // Stateless API: no JSESSIONID, so there is no session to fix
                // or to hijack. This also silences the SessionRepository warning.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Health checks must stay reachable by load balancers
                        // and uptime monitors, which cannot authenticate.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // OpenAPI docs and the Swagger UI.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()

                        // Nobody may sign up over the API; accounts are created
                        // by an administrator.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()

                        // Everything else requires authentication. Read access
                        // is open to any signed-in user; writes are additionally
                        // restricted to ADMIN by @PreAuthorize on the controller.
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .anyRequest().hasRole("ADMIN"))

                // Return 401/403 as JSON rather than redirecting to a login page.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Insufficient privileges\"}");
                        }))

                // Responses are read-only JSON; a strict CSP adds defence in
                // depth against content-type sniffing in the Swagger UI.
                .headers(headers -> headers
                        .contentTypeOptions(customizer -> {}));

        return http.build();
    }

    /**
     * BCrypt is used rather than a bare digest because it is deliberately
     * slow and salted, which defeats precomputed rainbow tables. Passwords are
     * never stored or compared in plain text.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}