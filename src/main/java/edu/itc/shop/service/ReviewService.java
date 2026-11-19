package edu.itc.shop.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Review;
import edu.itc.shop.repository.ReviewRepository;
import edu.itc.shop.web.dto.ReviewRequest;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Review create(String author, ReviewRequest request) {
        Review review = new Review(
                request.productId(),
                author,
                request.stars(),
                request.text()
        );
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> findByProductId(Long productId) {
        return reviewRepository.findByProductIdOrderByIdDesc(productId);
    }
}
