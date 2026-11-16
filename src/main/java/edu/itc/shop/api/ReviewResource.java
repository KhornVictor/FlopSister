package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import edu.itc.shop.api.dto.ReviewRequest;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Review;
import edu.itc.shop.domain.ReviewRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Sub-resource serving reviews for a resolved {@link Product}.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reviews", description = "Product reviews sub-resource")
public class ReviewResource {

    private final Product product;
    private final ReviewRepository reviewRepository;

    public ReviewResource(
            Product product,
            ReviewRepository reviewRepository) {
        this.product = product;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Lists all reviews written for the current product.
     */
    @GET
    @Operation(summary = "List product reviews", description = "Returns all reviews associated with a product.")
    @APIResponse(responseCode = "200", description = "Reviews retrieved")
    @APIResponse(responseCode = "404", description = "Parent product not found")
    @APIResponse(responseCode = "406", description = "Unsupported Accept media type")
    public List<Review> getReviews() {
        return reviewRepository.findByProductId(product.id());
    }

    /**
     * Creates a validated review for the current product and returns {@code 201 Created} with {@code Location}.
     */
    @POST
    @Operation(summary = "Add product review", description = "Creates a validated review for a product.")
    @APIResponse(responseCode = "201", description = "Review created")
    @APIResponse(responseCode = "400", description = "Invalid review payload")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Parent product not found")
    @APIResponse(responseCode = "415", description = "Unsupported Content-Type")
    public Response addReview(
            @Valid @NotNull ReviewRequest request,
            @Context UriInfo uriInfo) {

        Review review = reviewRepository.save(new Review(
                0,
                product.id(),
                request.author(),
                request.stars(),
                request.text()
        ));

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(review.id()))
                .build();

        return Response.created(location)
                .entity(review)
                .build();
    }
}
