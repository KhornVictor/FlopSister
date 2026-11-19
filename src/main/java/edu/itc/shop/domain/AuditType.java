package edu.itc.shop.domain;

public enum AuditType {
    LOGIN_OK,
    LOGIN_FAILED,
    LOGIN_RATE_LIMITED,
    LOCKED,
    TOKEN_REFRESHED,
    REFRESH_REUSE_DETECTED,
    LOGOUT,
    ACCESS_DENIED,
    ROLE_CHANGED
}
