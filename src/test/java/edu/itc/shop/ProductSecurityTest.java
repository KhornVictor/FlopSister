package edu.itc.shop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.shop.config.SecurityConfig;
import edu.itc.shop.domain.Product;
import edu.itc.shop.service.ProductNotFoundException;
import edu.itc.shop.service.ProductService;
import edu.itc.shop.web.ProductController;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService products;

    @Test
    void anonymousCanReadProductList() throws Exception {
        when(products.findAll()).thenReturn(List.of(new Product("USB-C cable", new BigDecimal("7.90"), 25)));

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
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""))
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
        mockMvc.perform(delete("/api/products/1").with(httpBasic("alice", "alice")))
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

        mockMvc.perform(delete("/api/products/1").with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonApiRequestIsDeniedEvenForAdmin() throws Exception {
        mockMvc.perform(get("/other").with(httpBasic("admin", "admin")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void missingProductReturns404ProblemJson() throws Exception {
        when(products.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidProductPostReturns400ProblemJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(httpBasic("alice", "alice"))
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
        when(products.create(any())).thenReturn(new Product("Mouse", new BigDecimal("19.90"), 5));

        mockMvc.perform(post("/api/products")
                        .with(httpBasic("alice", "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"price\":19.9,\"stock\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse"));
    }

    @Test
    void wrongBasicCredentialsReturn401WithOriginalInstance() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(httpBasic("alice", "wrong"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"price\":19.9,\"stock\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/products"));

        verifyNoInteractions(products);
    }
}
