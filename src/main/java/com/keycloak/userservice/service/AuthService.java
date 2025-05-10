package com.keycloak.userservice.service;

import com.keycloak.userservice.dto.AuthRequestDTO;
import com.keycloak.userservice.dto.AuthResponseDTO;
import com.keycloak.userservice.dto.RefreshTokenRequestDTO;
import com.keycloak.userservice.dto.RegistrationRequestDTO;
import com.keycloak.userservice.dto.KeycloakUserDTO;
import com.keycloak.userservice.dto.SimpleAuthRequestDTO;
import com.keycloak.userservice.event.UserEventType;
import com.keycloak.userservice.util.CreatedResponseUtil;
import com.keycloak.userservice.util.DistributedLockUtil;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final Keycloak adminKeycloak;
    private final String authServerUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final DistributedLockUtil lockUtil;
    private final UserEventService userEventService;

    private static final String LOGIN_LOCK_PREFIX = "lock:login:";
    private static final String REFRESH_LOCK_PREFIX = "lock:refresh:";
    private static final String REGISTER_LOCK_PREFIX = "lock:register:";

    @Autowired
    public AuthService(
            Keycloak adminKeycloak,
            @Value("${keycloak.auth-server-url}") String authServerUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.resource}") String clientId,
            @Value("${keycloak.credentials.secret}") String clientSecret,
            DistributedLockUtil lockUtil,
            UserEventService userEventService) {
        this.adminKeycloak = adminKeycloak;
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.lockUtil = lockUtil;
        this.userEventService = userEventService;
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        SimpleAuthRequestDTO simpleRequest = new SimpleAuthRequestDTO();
        simpleRequest.setUsername(request.username);
        simpleRequest.setPassword(request.password);
        
        String lockKey = LOGIN_LOCK_PREFIX + simpleRequest.getUsername();
        
        return lockUtil.executeWithLock(lockKey, () -> {
            try {
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm(realm)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .username(simpleRequest.getUsername())
                        .password(simpleRequest.getPassword())
                        .build();

                AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();
                
                List<UserRepresentation> users = adminKeycloak.realm(realm).users().search(simpleRequest.getUsername(), true);
                if (users.isEmpty()) {
                    userEventService.sendUserEvent(UserEventType.USER_LOGIN_FAILED, simpleRequest.getUsername(), 
                        Map.of("reason", "User not found"));
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
                }
                String userId = users.get(0).getId();
                
                List<String> roles = adminKeycloak.realm(realm).users().get(userId).roles()
                        .realmLevel().listAll().stream()
                        .map(RoleRepresentation::getName)
                        .collect(Collectors.toList());

                AuthResponseDTO response = new AuthResponseDTO();
                response.accessToken = tokenResponse.getToken();
                response.refreshToken = tokenResponse.getRefreshToken();
                response.tokenType = tokenResponse.getTokenType();
                response.expiresIn = tokenResponse.getExpiresIn();
                response.refreshExpiresIn = tokenResponse.getRefreshExpiresIn();
                response.userId = userId;
                response.roles = roles;

                userEventService.sendUserEvent(UserEventType.USER_LOGGED_IN, userId, 
                    Map.of("roles", roles, "tokenExpiresIn", tokenResponse.getExpiresIn()));
                
                return response;
            } catch (Exception e) {
                log.error("Ошибка при аутентификации пользователя", e);
                userEventService.sendUserEvent(UserEventType.USER_LOGIN_FAILED, request.username, 
                    Map.of("reason", e.getMessage()));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверные учетные данные");
            }
        });
    }

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String refreshToken = request.refreshToken;
        String lockKey = REFRESH_LOCK_PREFIX + refreshToken.hashCode();
        
        return lockUtil.executeWithLock(lockKey, () -> {
            try {
                Client client = ClientBuilder.newClient();
                WebTarget target = client.target(authServerUrl)
                        .path("/realms/" + realm + "/protocol/openid-connect/token");
                
                Form form = new Form();
                form.param("client_id", clientId);
                form.param("client_secret", clientSecret);
                form.param("grant_type", "refresh_token");
                form.param("refresh_token", refreshToken);
                
                Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));
                
                if (response.getStatus() != 200) {
                    userEventService.sendUserEvent(UserEventType.USER_LOGIN_FAILED, "unknown", 
                        Map.of("reason", "Invalid refresh token"));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Недействительный refresh токен");
                }
                
                AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
                
                AuthResponseDTO authResponse = new AuthResponseDTO();
                authResponse.accessToken = tokenResponse.getToken();
                authResponse.refreshToken = tokenResponse.getRefreshToken();
                authResponse.tokenType = tokenResponse.getTokenType();
                authResponse.expiresIn = tokenResponse.getExpiresIn();
                authResponse.refreshExpiresIn = tokenResponse.getRefreshExpiresIn();
                authResponse.roles = Collections.emptyList();

                userEventService.sendUserEvent(UserEventType.USER_LOGGED_IN, "unknown", 
                    Map.of("tokenExpiresIn", tokenResponse.getExpiresIn(), "isRefresh", true));
                
                return authResponse;
            } catch (Exception e) {
                log.error("Ошибка при обновлении токена", e);
                userEventService.sendUserEvent(UserEventType.USER_LOGIN_FAILED, "unknown", 
                    Map.of("reason", "Refresh token error: " + e.getMessage()));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Недействительный refresh токен");
            }
        });
    }

    public AuthResponseDTO register(RegistrationRequestDTO request) {
        String lockKey = REGISTER_LOCK_PREFIX + request.username;
        
        return lockUtil.executeWithLock(lockKey, () -> {
            try {
                UserRepresentation user = new UserRepresentation();
                user.setUsername(request.username);
                user.setEmail(request.email);
                user.setFirstName(request.firstName);
                user.setLastName(request.lastName);
                user.setEmailVerified(false);
                user.setEnabled(true);

                Map<String, List<String>> attributes = new HashMap<>();
                if (request.phoneNumber != null) {
                    attributes.put("phoneNumber", Collections.singletonList(request.phoneNumber));
                }
                user.setAttributes(attributes);

                Response response = adminKeycloak.realm(realm).users().create(user);
                String userId = CreatedResponseUtil.getCreatedId(response);
                
                if (userId == null) {
                    userEventService.sendUserEvent(UserEventType.USER_LOGIN_FAILED, request.username, 
                        Map.of("reason", "Failed to create user"));
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка при создании пользователя");
                }

                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setTemporary(false);
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(request.password);
                
                UserResource userResource = adminKeycloak.realm(realm).users().get(userId);
                userResource.resetPassword(passwordCred);

                RoleRepresentation userRole = adminKeycloak.realm(realm).roles().get("user").toRepresentation();
                userResource.roles().realmLevel().add(Collections.singletonList(userRole));

                userEventService.sendUserEvent(UserEventType.USER_REGISTERED, userId, 
                    Map.of("username", request.username, "email", request.email));

                AuthRequestDTO authRequest = new AuthRequestDTO();
                authRequest.username = request.username;
                authRequest.password = request.password;
                return login(authRequest);
                
            } catch (Exception e) {
                log.error("Ошибка при регистрации пользователя", e);
                userEventService.sendUserEvent(UserEventType.USER_LOGIN_FAILED, request.username, 
                    Map.of("reason", "Registration error: " + e.getMessage()));
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка при регистрации: " + e.getMessage());
            }
        });
    }
} 