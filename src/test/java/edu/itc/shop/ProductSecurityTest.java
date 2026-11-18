
package edu.itc.shop;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import edu.itc.shop.config.SecurityConfig;
import edu.itc.shop.domain.Product;
import edu.itc.shop.service.ProductNotFoundException;
import edu.itc.shop.service.ProductService;
import edu.itc.shop.web.ProductController;

@WebMvcTest(ProductController.class)
@Import({
        SecurityConfig.class,
        JwtTestSecurityConfiguration.class
})
class ProductSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService products;

    @Test
    void anonymousCanReadProductList() throws Exception {
        when(products.findAll()).thenReturn(
                List.of(new Product("USB-C cable", new BigDecimal("7.90"), 25))
        );

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("USB-C cable"));
    }

    @Test
    void anonymousPostReturns401ProblemJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"price\":19.9,\"stock\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value("/api/products"));

        verifyNoInteractions(products);
    }

    @Test
    void customerDeleteReturns403ProblemJson() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                        .with(aliceJwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.instance").value("/api/products/1"));

        verifyNoInteractions(products);
    }

    @Test
    void adminDeleteReturns204() throws Exception {
        doNothing().when(products).delete(1L);

        mockMvc.perform(delete("/api/products/1")
                        .with(adminJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonApiRequestIsDeniedEvenForAdmin() throws Exception {
        mockMvc.perform(get("/other")
                        .with(adminJwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void missingProductReturns404ProblemJson() throws Exception {
        when(products.findById(999L))
                .thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidProductPostReturns400ProblemJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(aliceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":0,\"stock\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(products);
    }

    @Test
    void authenticatedCustomerCanCreateProduct() throws Exception {
        when(products.create(any()))
                .thenReturn(new Product("Mouse", new BigDecimal("19.90"), 5));

        mockMvc.perform(post("/api/products")
                        .with(aliceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"price\":19.9,\"stock\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse"));
    }

    @Test
    void malformedBearerTokenReturns401WithOriginalInstance() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer not.a.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"price\":19.9,\"stock\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Token expired or invalid"))
                .andExpect(jsonPath("$.instance").value("/api/products"));

        verifyNoInteractions(products);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor aliceJwt() {
        return jwt()
                .jwt(token -> token.subject("alice").claim("roles", List.of("ROLE_CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(token -> token.subject("admin")
                        .claim("roles", List.of("ROLE_ADMIN", "ROLE_CUSTOMER")))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
