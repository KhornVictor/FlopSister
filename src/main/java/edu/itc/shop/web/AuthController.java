package edu.itc.shop.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.config.SecurityProps;
import edu.itc.shop.domain.AuditType;
import edu.itc.shop.service.AuditService;
import edu.itc.shop.service.RateLimitExceededException;
import edu.itc.shop.service.RateLimiterService;
import edu.itc.shop.service.RefreshTokenService;
import edu.itc.shop.web.dto.LoginRequest;
import edu.itc.shop.web.dto.RefreshTokenRequest;
import edu.itc.shop.web.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final SecurityProps securityProps;
    private final RefreshTokenService refreshTokenService;
    private final RateLimiterService rateLimiterService;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            SecurityProps securityProps,
            RefreshTokenService refreshTokenService,
            RateLimiterService rateLimiterService,
            UserDetailsService userDetailsService,
            AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.securityProps = securityProps;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiterService = rateLimiterService;
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        long retryAfter = rateLimiterService.tryConsume(clientIp, request.username());
        if (retryAfter > 0) {
            auditService.record(AuditType.LOGIN_RATE_LIMITED, request.username(), "retryAfter=" + retryAfter);
            throw new RateLimitExceededException(retryAfter);
        }

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(), request.password()));

        rateLimiterService.resetCounter(clientIp, authentication.getName());

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shop-api")
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(securityProps.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        var issued = refreshTokenService.issue(authentication.getName(), UUID.randomUUID());

        return new TokenResponse(
                token,
                "Bearer",
                securityProps.accessTokenTtl().toSeconds(),
                issued.rawToken(),
                RefreshTokenService.TTL.toSeconds()
        );
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var rotated = refreshTokenService.rotate(request.refreshToken());
        String username = rotated.entity().getUsername();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shop-api")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(securityProps.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new TokenResponse(
                token,
                "Bearer",
                securityProps.accessTokenTtl().toSeconds(),
                rotated.rawToken(),
                RefreshTokenService.TTL.toSeconds()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenService.revokeFamily(request.refreshToken());
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
