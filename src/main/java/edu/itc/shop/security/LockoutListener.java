package edu.itc.shop.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.AuditType;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AuditService;

@Component
public class LockoutListener {

    private final UserAccountRepository users;
    private final AuditService audit;

    public LockoutListener(UserAccountRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent e) {
        if (!(e.getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
            return;
        }
        String username = e.getAuthentication().getName();
        users.findByUsername(username).ifPresentOrElse(u -> {
            u.registerFailure();
            users.save(u);
            if (u.isLocked()) {
                audit.record(AuditType.LOCKED, username, "Account locked for 15 minutes after 5 consecutive failures");
            } else {
                audit.record(AuditType.LOGIN_FAILED, username, "Failure " + u.getFailedAttempts() + " of 5");
            }
        }, () -> {
            audit.record(AuditType.LOGIN_FAILED, username, "Unknown username");
        });
    }

    @EventListener
    @Transactional
    public void onLocked(AuthenticationFailureLockedEvent e) {
        if (!(e.getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
            return;
        }
        String username = e.getAuthentication().getName();
        audit.record(AuditType.LOCKED, username, "Login attempt on locked account");
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent e) {
        Authentication auth = e.getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            String username = auth.getName();
            users.findByUsername(username).ifPresent(u -> {
                u.resetLockout();
                users.save(u);
            });
            audit.record(AuditType.LOGIN_OK, username);
        }
    }

    @EventListener
    public void onAccessDenied(AuthorizationDeniedEvent<?> e) {
        Authentication auth = e.getAuthentication() != null && e.getAuthentication().get() != null
                ? e.getAuthentication().get()
                : null;
        String username = auth != null ? auth.getName() : "anonymous";
        audit.record(AuditType.ACCESS_DENIED, username, String.valueOf(e.getObject()));
    }
}
