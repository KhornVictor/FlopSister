package edu.itc.shop;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "JWT_SECRET=AuthFlowIT-test-secret-value-at-least-48-characters-long")
class AuthFlowIT {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JsonMapper jsonMapper;

    @LocalServerPort
    private int port;

    @Value("${shop.seed.customer-password}")
    private String customerPassword;

    @Value("${shop.seed.admin-password}")
    private String adminPassword;

    @Test
    void loginIssuesUsableJwtWithExpectedClaims() throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"" + customerPassword + "\"}", null);
        assertThat(login.statusCode()).isEqualTo(200);

        var tokenResponse = jsonMapper.readValue(login.body(),
                edu.itc.shop.web.dto.TokenResponse.class);
        assertThat(login.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(tokenResponse.tokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.expiresIn()).isEqualTo(900);

        String headerJson = new String(
                Base64.getUrlDecoder().decode(tokenResponse.token().split("\\.")[0]),
                StandardCharsets.UTF_8);
        assertThat(headerJson).contains("\"alg\":\"HS256\"");
        var decoded = jwtDecoder.decode(tokenResponse.token());
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("shop-api");
        assertThat(decoded.getSubject()).isEqualTo("alice");
        assertThat(UUID.fromString(decoded.getId())).isNotNull();
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("ROLE_CUSTOMER");
        assertThat(decoded.getExpiresAt().getEpochSecond() - decoded.getIssuedAt().getEpochSecond())
                .isEqualTo(900);

        HttpResponse<String> orders = send("GET", "/api/orders", null, tokenResponse.token());
        assertThat(orders.statusCode()).isEqualTo(200);
        assertThat(orders.body()).isEqualTo("[]");
        assertThat(orders.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void rolesClaimMapsToProductAdministratorAccess() throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + adminPassword + "\"}", null);
        assertThat(login.statusCode()).isEqualTo(200);
        var tokenResponse = jsonMapper.readValue(login.body(),
                edu.itc.shop.web.dto.TokenResponse.class);
        assertThat(jwtDecoder.decode(tokenResponse.token()).getClaimAsStringList("roles"))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");

        HttpResponse<String> deletion = send("DELETE", "/api/products/1", null,
                tokenResponse.token());
        assertThat(deletion.statusCode()).isEqualTo(204);
    }

    @Test
    void expiredJwtReturnsProblemJsonUnauthorized() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shop-api")
                .subject("alice")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        HttpResponse<String> response = send("GET", "/api/orders", null, expiredToken);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.headers().firstValue("WWW-Authenticate").orElse(""))
                .startsWith("Bearer error=\"invalid_token\"");
        assertThat(response.body()).contains("Token expired or invalid");
    }

    @Test
    void badPasswordReturnsGenericProblemJsonUnauthorized() throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"incorrect-password\"}", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.body()).contains("Bad credentials");
        assertThat(response.body()).doesNotContain("incorrect-password");
    }

    private HttpResponse<String> send(
            String method,
            String path,
            String body,
            String bearerToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (body != null) {
            request.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
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
