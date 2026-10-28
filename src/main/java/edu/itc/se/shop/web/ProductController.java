
package edu.itc.se.shop.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop.model.Product;
import edu.itc.se.shop.service.ProductService;
import edu.itc.se.shop.web.dto.CreateProduct;
import edu.itc.se.shop.web.dto.ProductDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductDto> list(
            @RequestParam(required = false) String q) {
        return service.search(q).stream()
                .map(ProductDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable long id) {
        return ProductDto.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(
            @RequestBody @Valid CreateProduct cmd) {
        Product product = service.create(cmd);
        ProductDto created = ProductDto.from(product);

        return ResponseEntity
                .created(URI.create("/api/products/" + created.id()))
                .body(created);
    }

    public record StockChange(
            @jakarta.validation.constraints.NotNull Integer delta) {}

    @PatchMapping("/{id}/stock")
    public ProductDto updateStock(
            @PathVariable long id,
            @RequestBody @Valid StockChange change) {
        return ProductDto.from(
                service.updateStock(id, change.delta()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
