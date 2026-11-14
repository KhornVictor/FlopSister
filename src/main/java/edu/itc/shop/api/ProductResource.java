package edu.itc.shop.api;

import java.util.List;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final ProductRepository repository;

    @Inject
    public ProductResource(ProductRepository repository) {
        this.repository = repository;
    }

    @GET
    public List<Product> findAll() {
        return repository.findAll();
    }

    @GET
    @Path("/{id: \\d+}")
    public Product findById(@PathParam("id") long id) {
        return repository.findById(id)
                .orElseThrow(NotFoundException::new);
    }
}
