package com.keycloak.userservice.service;

import com.keycloak.userservice.event.UserEvent;
import com.keycloak.userservice.event.UserEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user-service.events";

    public void sendUserEvent(UserEventType eventType, String userId, Object payload) {
        UserEvent event = createEvent(eventType, userId, payload);
        sendEvent(event);
    }

    private UserEvent createEvent(UserEventType eventType, String userId, Object payload) {
        HttpServletRequest request = getCurrentRequest();
        
        return new UserEvent(
            UUID.randomUUID(),
            eventType,
            userId,
            Instant.now(),
            payload,
            determineSource(request),
            getClientIp(request),
            getClientUserAgent(request)
        );
    }

    private void sendEvent(UserEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getUserId(), event);
            log.info("Sent event {} for user {}", event.getEventType(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to send event {} for user {}", event.getEventType(), event.getUserId(), e);
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String determineSource(HttpServletRequest request) {
        if (request == null) return "SYSTEM";
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.contains("Postman")) return "API";
        return "UI";
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getClientUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : "unknown";
    }

    // Методы для основных событий
    public void sendUserRegistered(String userId, Object payload) {
        sendUserEvent(UserEventType.USER_REGISTERED, userId, payload);
    }

    public void sendUserLoggedIn(String userId, Object payload) {
        sendUserEvent(UserEventType.USER_LOGGED_IN, userId, payload);
    }

    public void sendUserLoggedOut(String userId, Object payload) {
        sendUserEvent(UserEventType.USER_LOGGED_OUT, userId, payload);
    }

    public void sendProfileUpdated(String userId, Object payload) {
        sendUserEvent(UserEventType.PROFILE_UPDATED, userId, payload);
    }

    public void sendPasswordChanged(String userId, Object payload) {
        sendUserEvent(UserEventType.PASSWORD_CHANGED, userId, payload);
    }

    public void sendUserDeactivated(String userId, Object payload) {
        sendUserEvent(UserEventType.USER_DEACTIVATED, userId, payload);
    }

    public void sendUserActivated(String userId, Object payload) {
        sendUserEvent(UserEventType.USER_ACTIVATED, userId, payload);
    }

    public void sendUserDeleted(String userId, Object payload) {
        sendUserEvent(UserEventType.USER_DELETED, userId, payload);
    }

    public void sendRoleAssigned(String userId, Object payload) {
        sendUserEvent(UserEventType.ROLE_ASSIGNED, userId, payload);
    }

    public void sendRoleRemoved(String userId, Object payload) {
        sendUserEvent(UserEventType.ROLE_REMOVED, userId, payload);
    }

    public void sendSessionCreated(String userId, Object payload) {
        sendUserEvent(UserEventType.SESSION_CREATED, userId, payload);
    }

    public void sendSessionExpired(String userId, Object payload) {
        sendUserEvent(UserEventType.SESSION_EXPIRED, userId, payload);
    }
} 