package edu.itc.shop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import edu.itc.shop.domain.AuditType;
import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.repository.SecurityAuditRepository;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.RateLimiterService;
import edu.itc.shop.web.dto.TokenResponse;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "JWT_SECRET=AuthLifecycleIT-test-secret-value-at-least-48-characters-long")
class AuthLifecycleIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SecurityAuditRepository securityAuditRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Value("${shop.seed.customer-password}")
    private String customerPassword;

    @Value("${shop.seed.admin-password}")
    private String adminPassword;

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
        userAccountRepository.findByUsername("alice").ifPresent(user -> {
            user.resetLockout();
            userAccountRepository.save(user);
        });
    }

    @Test
    void refreshRotationReuseDetectionAndFamilyRevocationFlow() throws Exception {
        // 1. Alice logs in
        HttpResponse<String> loginRes = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"" + customerPassword + "\"}", null);
        assertThat(loginRes.statusCode()).isEqualTo(200);

        TokenResponse token1 = jsonMapper.readValue(loginRes.body(), TokenResponse.class);
        assertThat(token1.token()).isNotBlank();
        assertThat(token1.refreshToken()).isNotBlank();
        assertThat(token1.expiresIn()).isEqualTo(900);
        assertThat(token1.refreshExpiresIn()).isEqualTo(604800);

        // 2. Alice accesses /api/orders with access token 1
        HttpResponse<String> orders1 = send("GET", "/api/orders", null, token1.token());
        assertThat(orders1.statusCode()).isEqualTo(200);

        // 3. Rotate refresh token: exchange rt1 for new access token and rt2
        HttpResponse<String> refreshRes = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + token1.refreshToken() + "\"}", null);
        assertThat(refreshRes.statusCode()).isEqualTo(200);

        TokenResponse token2 = jsonMapper.readValue(refreshRes.body(), TokenResponse.class);
        assertThat(token2.token()).isNotBlank();
        assertThat(token2.refreshToken()).isNotBlank();
        assertThat(token2.refreshToken()).isNotEqualTo(token1.refreshToken());

        // 4. Access /api/orders with new access token 2
        HttpResponse<String> orders2 = send("GET", "/api/orders", null, token2.token());
        assertThat(orders2.statusCode()).isEqualTo(200);

        // 5. Presenting an already-rotated token (rt1) detects theft / reuse
        HttpResponse<String> reuseRes = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + token1.refreshToken() + "\"}", null);
        assertThat(reuseRes.statusCode()).isEqualTo(401);
        assertThat(reuseRes.body()).contains("Refresh token invalid");

        // Verify REFRESH_REUSE_DETECTED was audited
        var reuseAudits = securityAuditRepository.findByUsernameOrderByAtDesc("alice").stream()
                .filter(a -> a.getType() == AuditType.REFRESH_REUSE_DETECTED)
                .toList();
        assertThat(reuseAudits).isNotEmpty();

        // 6. The whole token family is revoked; now even token 2's refresh token is rejected
        HttpResponse<String> revokedFamilyRes = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + token2.refreshToken() + "\"}", null);
        assertThat(revokedFamilyRes.statusCode()).isEqualTo(401);
        assertThat(revokedFamilyRes.body()).contains("Refresh token invalid");
    }

    @Test
    void logoutRevokesRefreshTokenFamily() throws Exception {
        HttpResponse<String> loginRes = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"" + customerPassword + "\"}", null);
        assertThat(loginRes.statusCode()).isEqualTo(200);

        TokenResponse tokens = jsonMapper.readValue(loginRes.body(), TokenResponse.class);

        // Logout
        HttpResponse<String> logoutRes = send("POST", "/api/auth/logout",
                "{\"refreshToken\":\"" + tokens.refreshToken() + "\"}", null);
        assertThat(logoutRes.statusCode()).isEqualTo(204);

        // Refresh token should now be rejected
        HttpResponse<String> refreshAfterLogout = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + tokens.refreshToken() + "\"}", null);
        assertThat(refreshAfterLogout.statusCode()).isEqualTo(401);
    }

    @Test
    void accountLockoutAfterFiveConsecutiveFailures() throws Exception {
        // 5 failed login attempts from distributed IPs (to avoid single-IP rate limiting)
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> failRes = send("POST", "/api/auth/login",
                    "{\"username\":\"alice\",\"password\":\"wrong-password\"}", null, "198.51.100." + i);
            assertThat(failRes.statusCode()).isEqualTo(401);
        }

        // Verify user account is locked in the database
        UserAccount alice = userAccountRepository.findByUsername("alice").orElseThrow();
        assertThat(alice.isLocked()).isTrue();
        assertThat(alice.getFailedAttempts()).isGreaterThanOrEqualTo(5);

        // 6th attempt with correct password still returns generic 401 (no account state enumeration)
        HttpResponse<String> lockedAttempt = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"" + customerPassword + "\"}", null, "198.51.100.99");
        assertThat(lockedAttempt.statusCode()).isEqualTo(401);
        assertThat(lockedAttempt.body()).contains("Bad credentials");

        // Verify LOCKED event was audited
        var lockedAudits = securityAuditRepository.findByUsernameOrderByAtDesc("alice").stream()
                .filter(a -> a.getType() == AuditType.LOCKED)
                .toList();
        assertThat(lockedAudits).isNotEmpty();
    }

    @Test
    void rateLimiterReturns429WithRetryAfterHeader() throws Exception {
        // First 5 attempts consumed from same IP
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> res = send("POST", "/api/auth/login",
                    "{\"username\":\"alice\",\"password\":\"wrong\"}", null);
            assertThat(res.statusCode()).isEqualTo(401);
        }

        // 6th attempt is rate-limited
        HttpResponse<String> rateLimitedRes = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"wrong\"}", null);
        assertThat(rateLimitedRes.statusCode()).isEqualTo(429);
        assertThat(rateLimitedRes.headers().firstValue("Retry-After")).isPresent();
        assertThat(rateLimitedRes.body()).contains("Too many login attempts");

        // Verify LOGIN_RATE_LIMITED event was audited
        var rateLimitAudits = securityAuditRepository.findByUsernameOrderByAtDesc("alice").stream()
                .filter(a -> a.getType() == AuditType.LOGIN_RATE_LIMITED)
                .toList();
        assertThat(rateLimitAudits).isNotEmpty();
    }

    @Test
    void auditLogsPersistedAndAccessibleToAdminOnly() throws Exception {
        // Login as customer alice
        HttpResponse<String> aliceLogin = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"" + customerPassword + "\"}", null);
        TokenResponse aliceToken = jsonMapper.readValue(aliceLogin.body(), TokenResponse.class);

        // Customer alice is forbidden from reading audit logs (403)
        HttpResponse<String> customerAudit = send("GET", "/api/admin/audit", null, aliceToken.token());
        assertThat(customerAudit.statusCode()).isEqualTo(403);

        // Login as admin
        HttpResponse<String> adminLogin = send("POST", "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + adminPassword + "\"}", null);
        TokenResponse adminToken = jsonMapper.readValue(adminLogin.body(), TokenResponse.class);

        // Admin can read audit logs (200)
        HttpResponse<String> adminAudit = send("GET", "/api/admin/audit?username=alice", null, adminToken.token());
        assertThat(adminAudit.statusCode()).isEqualTo(200);
        assertThat(adminAudit.body()).contains("alice");
    }

    private HttpResponse<String> send(
            String method,
            String path,
            String body,
            String bearerToken) throws Exception {
        return send(method, path, body, bearerToken, null);
    }

    private HttpResponse<String> send(
            String method,
            String path,
            String body,
            String bearerToken,
            String clientIp) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (body != null) {
            request.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        if (clientIp != null) {
            request.header("X-Forwarded-For", clientIp);
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
