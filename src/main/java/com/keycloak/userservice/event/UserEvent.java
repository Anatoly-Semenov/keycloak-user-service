package com.keycloak.userservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private UUID eventId;
    private UserEventType eventType;
    private String userId;
    private Instant timestamp;
    private Object payload;
    private String source; // Источник события (например, "API", "UI", "SYSTEM")
    private String ipAddress; // IP-адрес, с которого произошло событие
    private String userAgent; // User-Agent клиента
} 