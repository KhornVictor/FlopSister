
package edu.itc.shop;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
        JwtTestSecurityConfiguration.class
})
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

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
                        .with(jwt()
                                .jwt(token -> token.subject("alice")
                                        .claim("roles", List.of("ROLE_CUSTOMER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(orderService);
    }

    @Test
    void adminCanShipOrder() throws Exception {

        mockMvc.perform(post("/api/orders/1/ship")
                        .with(jwt()
                                .jwt(token -> token.subject("admin")
                                        .claim("roles", List.of("ROLE_ADMIN", "ROLE_CUSTOMER")))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isNoContent());

        verify(orderService).ship(1L);
    }
}
