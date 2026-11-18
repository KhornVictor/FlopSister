package edu.itc.shop.config;

import java.io.IOException;
import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security configuration for the stateless API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(
            HttpSecurity http,
            JsonMapper jsonMapper,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        AuthenticationEntryPoint entryPoint = (req, res, ex) -> {
            String authorization = req.getHeader(HttpHeaders.AUTHORIZATION);
            boolean bearerRequest = authorization != null && authorization.startsWith("Bearer ");
            if (bearerRequest) {
                bearerEntryPoint.commence(req, res, ex);
            } else {
                res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            }
            String detail = bearerRequest ? "Token expired or invalid" : "Authentication required";
            writeProblem(res, req, jsonMapper, HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
        };

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/ship").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(form -> form.disable())
                // CSRF disabled: this chain accepts Bearer tokens only, never cookies.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(entryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((req, res, ex) -> writeProblem(res, req, jsonMapper,
                                HttpStatus.FORBIDDEN, "Forbidden", "Access denied")));

        return http.build();
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
