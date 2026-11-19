package edu.itc.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private int stars;

    @Column(length = 1000, nullable = false)
    private String text;

    protected Review() {
    }

    public Review(Long productId, String author, int stars, String text) {
        this.productId = productId;
        this.author = author;
        this.stars = stars;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getAuthor() {
        return author;
    }

    public int getStars() {
        return stars;
    }

    public String getText() {
        return text;
    }
}
