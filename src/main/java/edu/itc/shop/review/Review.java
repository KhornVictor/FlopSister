
package edu.itc.shop.review;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private int stars;

    @Column(length = 1000, nullable = false)
    private String content;

    protected Review() {
        // Required by JPA
    }

    // Constructor for the existing HTML demo
    public Review(Long productId, String content) {
        this.productId = productId;
        this.content = content;
    }

    // Constructor for the REST API
    public Review(Long productId, int stars, String content) {
        this.productId = productId;
        this.stars = stars;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getStars() {
        return stars;
    }

    public String getContent() {
        return content;
    }
}
