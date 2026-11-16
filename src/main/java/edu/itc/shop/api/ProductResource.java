package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.ProductV2;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.api.params.ProductFilter;
import edu.itc.shop.domain.DuplicateSkuException;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import edu.itc.shop.domain.ReviewRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductRepository repository;
    private final ReviewRepository reviewRepository;

   @Inject
public ProductResource(
        ProductRepository repository,
        ReviewRepository reviewRepository) {

    this.repository = repository;
    this.reviewRepository = reviewRepository;
}

   @GET
public Response list(
        @BeanParam @Valid ProductFilter filter,
        @Context UriInfo uriInfo) {

    List<Product> filtered = filter.filter(repository.findAll());

    int total = filtered.size();

    int lastPage = Math.max(0, (total - 1) / filter.size);

    long start = (long) filter.page * filter.size;

    int fromIndex = (int) Math.min(start, total);
    int toIndex = Math.min(fromIndex + filter.size, total);

    List<ProductDto> pageContent = filtered.subList(fromIndex, toIndex)
            .stream()
            .map(ProductDto::from)
            .toList();

    List<String> links = new java.util.ArrayList<>();

    links.add(createPageLink(uriInfo, 0, "first"));

    if (filter.page > 0) {
        links.add(createPageLink(uriInfo, filter.page - 1, "prev"));
    }

    if (filter.page < lastPage) {
        links.add(createPageLink(uriInfo, filter.page + 1, "next"));
    }

    links.add(createPageLink(uriInfo, lastPage, "last"));

    return Response.ok(pageContent)
            .header("X-Total-Count", total)
            .header("Link", String.join(", ", links))
            .build();
}
private String createPageLink(UriInfo uriInfo, int page, String rel) {
    String uri = uriInfo.getRequestUriBuilder()
            .replaceQueryParam("page", page)
            .build()
            .toString();

    return "<" + uri + ">; rel=\"" + rel + "\"";
}
@Produces({
    MediaType.APPLICATION_JSON + "; qs=1.0",
    "application/vnd.shop.v1+json",
    MediaType.APPLICATION_XML
})
   @GET
@Path("/{id: \\d+}")
public Response findById(
        @PathParam("id") long id,
        @Context Request request) {

    Product product = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));

    EntityTag tag = new EntityTag(
            Integer.toHexString(product.version())
    );

    Response.ResponseBuilder precondition =
            request.evaluatePreconditions(tag);

    if (precondition != null) {
        return precondition.tag(tag).build();
    }

    CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate(true);
    cacheControl.setMaxAge(60);
    cacheControl.setNoTransform(true);

    return Response.ok(ProductDto.from(product))
            .tag(tag)
            .cacheControl(cacheControl)
            .build();
}

@GET
@Path("/{id: \\d+}")
@Produces("application/vnd.shop.v2+json")
public ProductV2 findByIdV2(@PathParam("id") long id) {
    Product product = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));

    return ProductV2.from(product);
}

    @POST
    public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
        if (dto.sku != null) {
            repository.findBySku(dto.sku).ifPresent(p -> {
                throw new DuplicateSkuException(dto.sku);
            });
        }
        Product saved = repository.save(dto.toProduct(0, 0));
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(Long.toString(saved.id()))
                .build();
        return Response.created(location)
                .entity(ProductDto.from(saved))
                .build();
    }


@PUT
@Path("/{id: \\d+}")
public Response update(
        @PathParam("id") long id,
        @Valid @NotNull ProductDto dto,
        @HeaderParam("If-Match") String ifMatch) {

    if (ifMatch == null || ifMatch.isBlank()) {
        return Response.status(428).build();
    }

    Product existing = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));

    EntityTag currentTag = new EntityTag(
            Integer.toHexString(existing.version())
    );

    if (!"*".equals(ifMatch.trim())
            && !currentTag.toString().equals(ifMatch.trim())) {
        return Response.status(Response.Status.PRECONDITION_FAILED).build();
    }

    if (dto.sku != null) {
        repository.findBySku(dto.sku)
                .filter(p -> p.id() != id)
                .ifPresent(p -> {
                    throw new DuplicateSkuException(dto.sku);
                });
    }

    Product updated = repository.save(dto.toProduct(id, existing.version()));

    EntityTag newTag = new EntityTag(
            Integer.toHexString(updated.version())
    );

    return Response.ok(ProductDto.from(updated))
            .tag(newTag)
            .build();
}

    @DELETE
    @Path("/{id: \\d+}")
    public Response delete(@PathParam("id") long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException("Product " + id + " not found");
        }
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id: \\d+}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDto updateStock(
            @PathParam("id") long id,
            @Valid @NotNull StockChange change) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        Product updated = repository.save(existing.adjustStock(change.delta()));
        return ProductDto.from(updated);
    }

@Path("/{id: \\d+}/reviews")
public ReviewResource reviews(@PathParam("id") long id) {

    Product product = repository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException("Product " + id + " not found"));

    return new ReviewResource(product, reviewRepository);
}

}
