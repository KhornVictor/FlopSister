
package edu.itc.shop;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.shop.config.SecurityConfig;
import edu.itc.shop.service.OrderService;
import edu.itc.shop.web.OrderController;

@WebMvcTest(OrderController.class)
@Import({
        SecurityConfig.class,
        OrderControllerSecurityTest.TestUsersConfiguration.class
})
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestUsersConfiguration {

        @Bean
        PasswordEncoder testPasswordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        InMemoryUserDetailsManager testUsers(PasswordEncoder encoder) {

            var alice = User.withUsername("alice")
                    .password(encoder.encode("alice"))
                    .roles("CUSTOMER")
                    .build();

            var admin = User.withUsername("admin")
                    .password(encoder.encode("admin"))
                    .roles("ADMIN", "CUSTOMER")
                    .build();

            return new InMemoryUserDetailsManager(alice, admin);
        }
    }

    @Test
    void anonymousCannotAccessOrderHistory() throws Exception {

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(orderService);
    }

    @Test
    void customerCannotShipOrder() throws Exception {

        mockMvc.perform(post("/api/orders/1/ship")
                        .with(httpBasic("alice", "alice")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(orderService);
    }

    @Test
    void adminCanShipOrder() throws Exception {

        mockMvc.perform(post("/api/orders/1/ship")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());

        verify(orderService).ship(1L);
    }
}
