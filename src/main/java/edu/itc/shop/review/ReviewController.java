package edu.itc.shop.review;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewRepository reviewRepository;

    public ReviewController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/demo/reviews")
    public String showReviews(Model model) {
        model.addAttribute("reviews",
                reviewRepository.findByProductIdOrderByIdDesc(1L));

        return "reviews";
    }

    // @PostMapping("/demo/reviews")
    // public String addReview(
    //         @RequestParam String content,
    //         RedirectAttributes redirectAttributes) {

    //     reviewRepository.save(new Review(1L, content));

    //     redirectAttributes.addFlashAttribute(
    //             "message", "Review submitted successfully!");

    //     return "redirect:/demo/reviews";
    // }
}