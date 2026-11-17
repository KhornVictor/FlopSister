package edu.itc.shop;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.shop.domain.Role;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.security.JpaUserDetailsService;

@SpringBootTest
@AutoConfigureMockMvc
class AccountIntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JpaUserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("seedData")
    private CommandLineRunner seedData;

    @Value("${shop.seed.admin-password}")
    private String adminPassword;

    @Value("${shop.seed.customer-password}")
    private String customerPassword;

    @Test
    void seededAccountsHaveCorrectRolesAndBcryptPasswords() {
        var admin = userAccountRepository.findByUsername("admin").orElseThrow();
        var alice = userAccountRepository.findByUsername("alice").orElseThrow();

        // Display the actual persisted database row (encoded hash only).
        System.out.println("DATABASE ROW: username="
                + alice.getUsername()
                + ", password_hash="
                + alice.getPasswordHash());

        assertThat(admin.getRoles())
                .containsExactlyInAnyOrder(Role.ADMIN, Role.CUSTOMER);

        assertThat(alice.getRoles())
                .containsExactly(Role.CUSTOMER);

        assertThat(admin.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(alice.getPasswordHash()).startsWith("{bcrypt}");

        assertThat(passwordEncoder.matches(adminPassword, admin.getPasswordHash()))
                .isTrue();

        assertThat(passwordEncoder.matches(customerPassword, alice.getPasswordHash()))
                .isTrue();
    }

    @Test
    void jpaUserDetailsServiceLoadsCorrectAuthorities() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");
        UserDetails alice = userDetailsService.loadUserByUsername("alice");

        assertThat(admin.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");

        assertThat(alice.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");

        assertThat(admin.isEnabled()).isTrue();
        assertThat(alice.isEnabled()).isTrue();
    }

    @Test
    void registrationCreatesCustomerWithBcryptPassword() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().substring(0, 8);
        String rawPassword = "StrongPass123";

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, rawPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var account = userAccountRepository.findByUsername(username).orElseThrow();

        assertThat(account.getRoles()).containsExactly(Role.CUSTOMER);
        assertThat(account.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(rawPassword, account.getPasswordHash()))
                .isTrue();
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        String username = "duplicate_" + UUID.randomUUID().toString().substring(0, 8);

        String request = """
                {
                  "username": "%s",
                  "password": "StrongPass123"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invalidRegistrationReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Bad Name",
                                  "password": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void runningSeedAgainDoesNotDuplicateAccounts() throws Exception {
        var adminId = userAccountRepository.findByUsername("admin")
                .orElseThrow().getId();

        var aliceId = userAccountRepository.findByUsername("alice")
                .orElseThrow().getId();

        long countBefore = userAccountRepository.count();

        seedData.run();

        long countAfter = userAccountRepository.count();

        assertThat(countAfter).isEqualTo(countBefore);

        assertThat(userAccountRepository.findByUsername("admin")
                .orElseThrow().getId()).isEqualTo(adminId);

        assertThat(userAccountRepository.findByUsername("alice")
                .orElseThrow().getId()).isEqualTo(aliceId);
    }
}
