package edu.itc.shop.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.ClientErrorException;
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
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private static final String V1_MEDIA_TYPE = "application/vnd.shop.v1+json";
    private static final String V2_MEDIA_TYPE = "application/vnd.shop.v2+json";
    private static final Response.StatusType PRECONDITION_REQUIRED = new Response.StatusType() {
        @Override
        public int getStatusCode() {
            return 428;
        }

        @Override
        public Response.Status.Family getFamily() {
            return Response.Status.Family.CLIENT_ERROR;
        }

        @Override
        public String getReasonPhrase() {
            return "Precondition Required";
        }
    };

    private final ProductRepository repository;
    private final ReviewRepository reviewRepository;

    @Inject
    public ProductResource(ProductRepository repository, ReviewRepository reviewRepository) {
        this.repository = repository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Returns a paginated, filtered, and sorted product page with {@code X-Total-Count} and {@code Link} headers.
     */
    @GET
    public Response list(@BeanParam @Valid ProductFilter filter, @Context UriInfo uriInfo) {
        List<Product> all = filter.filter(repository.findAll());
        int total = all.size();
        int lastPage = Math.max(0, (total - 1) / filter.size);
        List<ProductDto> page = all.stream()
                .skip((long) filter.page * filter.size)
                .limit(filter.size)
                .map(ProductDto::from)
                .toList();

        var links = new ArrayList<Link>();
        links.add(pageLink(uriInfo, 0, "first"));
        if (filter.page > 0) {
            links.add(pageLink(uriInfo, Math.min(filter.page - 1, lastPage), "prev"));
        }
        if (filter.page < lastPage) {
            links.add(pageLink(uriInfo, filter.page + 1, "next"));
        }
        links.add(pageLink(uriInfo, lastPage, "last"));

        return Response.ok(new GenericEntity<List<ProductDto>>(page) {})
                .links(links.toArray(Link[]::new))
                .header("X-Total-Count", total)
                .build();
    }

    /**
     * Returns v1 (JSON or XML) representation of a product with {@code ETag}, {@code Cache-Control}, and {@code 304} support.
     */
    @GET
    @Path("/{id: \\d+}")
    @Produces({"application/json; qs=1.0", V1_MEDIA_TYPE, MediaType.APPLICATION_XML})
    public Response findById(@PathParam("id") long id, @Context Request request) {
        return cachedProductResponse(id, request, ProductDto::from);
    }

    /**
     * Returns v2 representation of a product selected via {@code Accept: application/vnd.shop.v2+json}.
     */
    @GET
    @Path("/{id: \\d+}")
    @Produces(V2_MEDIA_TYPE)
    public Response findByIdV2(@PathParam("id") long id, @Context Request request) {
        return cachedProductResponse(id, request, ProductV2::from);
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

    /**
     * Replaces a product only when {@code If-Match} matches its current {@code ETag}.
     */
    @PUT
    @Path("/{id: \\d+}")
    public Response update(
            @PathParam("id") long id,
            @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch,
            @Context Request request,
            @Valid @NotNull ProductDto dto) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ClientErrorException(
                    "If-Match header is required",
                    Response.status(PRECONDITION_REQUIRED).build());
        }
        Product existing = requireProduct(id);
        EntityTag tag = entityTag(existing);
        Response.ResponseBuilder precondition = evaluateIfMatch(request, tag);
        if (precondition != null) {
            return precondition.tag(tag).build();
        }
        if (dto.sku != null) {
            repository.findBySku(dto.sku)
                    .filter(p -> p.id() != id)
                    .ifPresent(p -> {
                        throw new DuplicateSkuException(dto.sku);
                    });
        }
        Product updated = repository.save(dto.toProduct(id, existing.version()));
        return Response.ok(ProductDto.from(updated))
                .tag(entityTag(updated))
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
        Product existing = requireProduct(id);
        Product updated = repository.save(existing.adjustStock(change.delta()));
        return ProductDto.from(updated);
    }

    /**
     * Sub-resource locator delegating {@code /products/{id}/reviews} to {@link ReviewResource}.
     */
    @Path("/{id: \\d+}/reviews")
    public ReviewResource reviews(@PathParam("id") long id) {
        return new ReviewResource(requireProduct(id), reviewRepository);
    }

    private Response cachedProductResponse(long id, Request request, Function<Product, ?> mapper) {
        Product product = requireProduct(id);
        EntityTag tag = entityTag(product);
        Response.ResponseBuilder conditional = request.evaluatePreconditions(tag);
        if (conditional != null) {
            return conditional.tag(tag).build();
        }
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.setMaxAge(60);
        cc.setNoTransform(true);
        return Response.ok(mapper.apply(product))
                .tag(tag)
                .cacheControl(cc)
                .build();
    }

    private Product requireProduct(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    private static EntityTag entityTag(Product product) {
        return new EntityTag(Integer.toHexString(product.version()));
    }

    private static Response.ResponseBuilder evaluateIfMatch(Request request, EntityTag tag) {
        try {
            return request.evaluatePreconditions(tag);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid If-Match header", exception);
        }
    }

    private static Link pageLink(UriInfo uriInfo, int page, String rel) {
        return Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", page))
                .rel(rel)
                .build();
    }
}
