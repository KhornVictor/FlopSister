package edu.itc.shop.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_audit")
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant at;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditType type;

    @Column(length = 60)
    private String username;

    @Column(length = 60)
    private String ip;

    @Column(length = 60)
    private String requestId;

    @Column(length = 1000)
    private String detail;

    protected SecurityAudit() {
    }

    public SecurityAudit(Instant at, AuditType type, String username, String ip, String requestId, String detail) {
        this.at = at;
        this.type = type;
        this.username = username;
        this.ip = ip;
        this.requestId = requestId;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Instant getAt() {
        return at;
    }

    public AuditType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getDetail() {
        return detail;
    }
}
