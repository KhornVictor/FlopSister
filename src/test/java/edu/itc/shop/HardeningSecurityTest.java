package edu.itc.shop;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class HardeningSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE"))
                .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void corsRejectsUnauthorizedOrigin() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void storedXssPayloadIsStoredAndEscapedInHtml() throws Exception {
        String jsonPayload = "{\"productId\":2,\"stars\":1,\"text\":\"<img src=x onerror=\\\"document.title='pwned'\\\">\"}";

        // Anonymous POST to /api/reviews is unauthorized
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized());

        // Authenticated alice submits the review
        mockMvc.perform(post("/api/reviews")
                        .with(jwt()
                                .jwt(t -> t.subject("alice").claim("roles", List.of("ROLE_CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.stars").value(1))
                .andExpect(jsonPath("$.text").value("<img src=x onerror=\"document.title='pwned'\">"));

        // Public JSON endpoint returns escaped JSON
        mockMvc.perform(get("/api/products/2/reviews"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$[0].author").value("alice"))
                .andExpect(jsonPath("$[0].text").value("<img src=x onerror=\"document.title='pwned'\">"));

        // Public Thymeleaf HTML page safely escapes the payload
        mockMvc.perform(get("/products/2/reviews.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "&lt;img src=x onerror=&quot;document.title=&#39;pwned&#39;&quot;&gt;")));
    }
}
