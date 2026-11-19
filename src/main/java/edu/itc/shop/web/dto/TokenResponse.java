package edu.itc.shop.web.dto;

public record TokenResponse(
        String token,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn) {

    public TokenResponse(String token, String tokenType, long expiresIn) {
        this(token, tokenType, expiresIn, null, 0);
    }
}
