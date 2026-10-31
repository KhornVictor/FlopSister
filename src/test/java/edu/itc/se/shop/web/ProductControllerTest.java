
package edu.itc.se.shop.web;

import edu.itc.se.shop.model.Product;
import edu.itc.se.shop.service.ProductService;
import edu.itc.se.shop.service.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService service;

    @Test
    void listReturnsProducts() throws Exception {
        when(service.search(null)).thenReturn(List.of(
                new Product(1, "USB-C cable",
                        new BigDecimal("7.90"), 25)));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("USB-C cable"));
    }

    @Test
    void missingProductReturns404() throws Exception {
        when(service.findById(999))
                .thenThrow(new ProductNotFoundException(999));

        mvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("Product 999 not found"));
    }

    @Test
    void invalidCreateReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","price":0,"stock":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists());
    }

    @Test
    void validCreateReturns201AndLocation() throws Exception {
        when(service.create(any())).thenReturn(
                new Product(6, "USB-C cable",
                        new BigDecimal("7.90"), 25));

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"USB-C cable","price":7.90,"stock":25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/products/6"))
                .andExpect(jsonPath("$.id").value(6));
    }
}
