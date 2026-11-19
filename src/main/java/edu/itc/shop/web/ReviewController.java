package edu.itc.shop.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.itc.shop.domain.Review;
import edu.itc.shop.service.ReviewService;
import edu.itc.shop.web.dto.ReviewRequest;
import jakarta.validation.Valid;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/reviews")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Review createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return reviewService.create(authentication.getName(), request);
    }

    @GetMapping("/api/products/{id}/reviews")
    @ResponseBody
    public List<Review> getReviews(@PathVariable Long id) {
        return reviewService.findByProductId(id);
    }

    @GetMapping("/products/{id}/reviews.html")
    public String renderReviewsPage(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        model.addAttribute("reviews", reviewService.findByProductId(id));
        return "reviews";
    }
}
