package edu.itc.shop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.AuditType;
import edu.itc.shop.domain.RefreshToken;
import edu.itc.shop.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    public static final Duration TTL = Duration.ofDays(7);
    private final RefreshTokenRepository repo;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public record Issued(String rawToken, RefreshToken entity) {}

    public RefreshTokenService(RefreshTokenRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional
    public Issued issue(String username, UUID family) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken t = new RefreshToken(sha256(raw), username, family, Instant.now().plus(TTL));
        return new Issued(raw, repo.save(t));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Issued rotate(String raw) {
        RefreshToken current = repo.findByTokenHash(sha256(raw))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token invalid"));

        if (current.isRevoked()) {
            repo.revokeFamily(current.getFamilyId(), Instant.now());
            audit.record(AuditType.REFRESH_REUSE_DETECTED, current.getUsername(), "family=" + current.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token invalid");
        }

        if (current.isExpired()) {
            audit.record(AuditType.LOGIN_FAILED, current.getUsername(), "expired refresh token family=" + current.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token invalid");
        }

        Issued next = issue(current.getUsername(), current.getFamilyId());
        current.revoke(next.entity().getId());
        repo.save(current);
        audit.record(AuditType.TOKEN_REFRESHED, current.getUsername(), "family=" + current.getFamilyId());
        return next;
    }

    @Transactional
    public void revokeFamily(String raw) {
        repo.findByTokenHash(sha256(raw)).ifPresent(token -> {
            repo.revokeFamily(token.getFamilyId(), Instant.now());
            audit.record(AuditType.LOGOUT, token.getUsername(), "family=" + token.getFamilyId());
        });
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
