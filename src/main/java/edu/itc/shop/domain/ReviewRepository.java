package edu.itc.shop.domain;

import java.util.List;

/**
 * Persistence contract for product reviews.
 */
public interface ReviewRepository {

    /**
     * Returns all reviews belonging to the given product, ordered deterministically.
     */
    List<Review> findByProductId(long productId);

    /**
     * Persists a review, assigning an ID when {@code review.id() == 0}.
     */
    Review save(Review review);
}
