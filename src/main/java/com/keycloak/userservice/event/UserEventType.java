package com.keycloak.userservice.event;

public enum UserEventType {
    // События регистрации и аутентификации
    USER_REGISTERED,
    USER_LOGGED_IN,
    USER_LOGGED_OUT,
    USER_LOGIN_FAILED,
    
    // События профиля
    PROFILE_UPDATED,
    PROFILE_VIEWED,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    
    // События безопасности
    TWO_FACTOR_ENABLED,
    TWO_FACTOR_DISABLED,
    SECURITY_SETTINGS_CHANGED,
    
    // События состояния пользователя
    USER_DEACTIVATED,
    USER_ACTIVATED,
    USER_DELETED,
    USER_BLOCKED,
    USER_UNBLOCKED,
    
    // События ролей и разрешений
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    
    // События сессий
    SESSION_CREATED,
    SESSION_EXPIRED,
    SESSION_TERMINATED,
    
    // События уведомлений
    NOTIFICATION_SENT,
    NOTIFICATION_READ,
    
    // События API
    API_ACCESS_GRANTED,
    API_ACCESS_REVOKED,
    API_KEY_CREATED,
    API_KEY_DELETED
} 