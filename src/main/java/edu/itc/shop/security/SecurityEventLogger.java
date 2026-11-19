package edu.itc.shop.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY");

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        log.info("login.ok user={} type={}",
                event.getAuthentication().getName(),
                event.getAuthentication().getClass().getSimpleName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        String user = (auth instanceof BearerTokenAuthenticationToken)
                ? "<bearer-token>"
                : (auth != null ? auth.getName() : "unknown");
        log.warn("login.failed user={} reason={}",
                user, event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = event.getAuthentication() != null && event.getAuthentication().get() != null
                ? event.getAuthentication().get()
                : null;
        String user = (auth != null) ? auth.getName() : "anonymous";
        log.warn("access.denied user={} object={}", user, event.getObject());
    }
}
