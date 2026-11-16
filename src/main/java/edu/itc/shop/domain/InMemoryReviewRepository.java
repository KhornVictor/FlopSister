package edu.itc.shop.domain;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory implementation of {@link ReviewRepository}.
 */
public class InMemoryReviewRepository implements ReviewRepository {

    private final ConcurrentMap<Long, Review> reviews = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    @Override
    public List<Review> findByProductId(long productId) {
        return reviews.values().stream()
                .filter(review -> review.productId() == productId)
                .sorted(Comparator.comparingLong(Review::id))
                .toList();
    }

    @Override
    public Review save(Review review) {
        long id = review.id() == 0
                ? nextId.incrementAndGet()
                : review.id();

        Review saved = new Review(
                id,
                review.productId(),
                review.stars(),
                review.author(),
                review.text());

        reviews.put(id, saved);
        nextId.accumulateAndGet(id, Math::max);
        return saved;
    }
}
