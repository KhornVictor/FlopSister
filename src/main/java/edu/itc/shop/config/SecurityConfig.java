package edu.itc.shop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security configuration for the Task 1 HTTP Basic stateless API filter chain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                // CSRF disabled: this chain accepts no cookie-based credentials (Basic / Bearer only)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\"");
                            writeProblem(res, req, jsonMapper, HttpStatus.UNAUTHORIZED, "Unauthorized",
                                    "Authentication required");
                        })
                        .accessDeniedHandler((req, res, ex) -> writeProblem(res, req, jsonMapper,
                                HttpStatus.FORBIDDEN, "Forbidden", "Access denied")));
        return http.build();
    }

    @Bean
    UserDetailsService temporaryUsers() { // removed in Task 2
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password("{noop}admin").roles("ADMIN").build(),
                User.withUsername("alice").password("{noop}alice").roles("CUSTOMER").build());
    }

    private static void writeProblem(HttpServletResponse response, HttpServletRequest request,
            JsonMapper jsonMapper, HttpStatus status, String title, String detailText) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detailText);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
