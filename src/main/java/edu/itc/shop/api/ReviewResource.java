
package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import edu.itc.shop.api.dto.ReviewRequest;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Review;
import edu.itc.shop.domain.ReviewRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class ReviewResource {

    private final Product product;
    private final ReviewRepository reviewRepository;

    public ReviewResource(
            Product product,
            ReviewRepository reviewRepository) {
        this.product = product;
        this.reviewRepository = reviewRepository;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Review> getReviews() {
        return reviewRepository.findByProductId(product.id());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReview(
            @Valid @NotNull ReviewRequest request,
            @Context UriInfo uriInfo) {

        Review review = reviewRepository.save(
                product.id(),
                request.author(),
                request.stars(),
                request.text()
        );

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(review.id()))
                .build();

        return Response.created(location)
                .entity(review)
                .build();
    }
}
