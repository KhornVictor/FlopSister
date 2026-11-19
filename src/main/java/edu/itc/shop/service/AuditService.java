package edu.itc.shop.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.itc.shop.domain.AuditType;
import edu.itc.shop.domain.SecurityAudit;
import edu.itc.shop.repository.SecurityAuditRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditService {

    private final SecurityAuditRepository repo;

    public AuditService(SecurityAuditRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAudit record(AuditType type, String username, String ip, String requestId, String detail) {
        SecurityAudit audit = new SecurityAudit(Instant.now(), type, username, ip, requestId, detail);
        return repo.save(audit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAudit record(AuditType type, String username, String detail) {
        String ip = currentClientIp();
        String rid = MDC.get("rid");
        return record(type, username, ip, rid, detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAudit record(AuditType type, String username) {
        return record(type, username, null);
    }

    @Transactional(readOnly = true)
    public List<SecurityAudit> find(String username) {
        if (username != null && !username.isBlank()) {
            return repo.findByUsernameOrderByAtDesc(username);
        }
        return repo.findAllByOrderByAtDesc();
    }

    private static String currentClientIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "127.0.0.1";
    }
}
