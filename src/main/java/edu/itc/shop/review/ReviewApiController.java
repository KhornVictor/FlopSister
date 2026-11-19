
package edu.itc.shop.review;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewApiController {

    private final ReviewRepository reviewRepository;

    public ReviewApiController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // Authenticated users can submit reviews
    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Review createReview(
            @Valid @RequestBody ReviewRequest request) {

        Review review = new Review(
                request.productId(),
                request.stars(),
                request.text()
        );

        return reviewRepository.save(review);
    }

    // Anyone can view reviews for a product
    @GetMapping("/api/products/{id}/reviews")
    public List<Review> getReviews(@PathVariable Long id) {
        return reviewRepository.findByProductIdOrderByIdDesc(id);
    }
}
