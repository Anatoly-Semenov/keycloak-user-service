package com.keycloak.userservice.service;

import com.keycloak.userservice.dto.AuthRequestDTO;
import com.keycloak.userservice.dto.AuthResponseDTO;
import com.keycloak.userservice.dto.RefreshTokenRequestDTO;
import com.keycloak.userservice.dto.RegistrationRequestDTO;
import com.keycloak.userservice.dto.KeycloakUserDTO;
import com.keycloak.userservice.util.CreatedResponseUtil;
import com.keycloak.userservice.util.DistributedLockUtil;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthService {
    private final Keycloak adminKeycloak;
    private final String authServerUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final DistributedLockUtil lockUtil;

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
            DistributedLockUtil lockUtil) {
        this.adminKeycloak = adminKeycloak;
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.lockUtil = lockUtil;
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        String lockKey = LOGIN_LOCK_PREFIX + request.getUsername();
        
        return lockUtil.executeWithLock(lockKey, () -> {
            try {
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm(realm)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .username(request.getUsername())
                        .password(request.getPassword())
                        .build();

                AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();
                
                // Получаем информацию о пользователе через админский клиент
                List<UserRepresentation> users = adminKeycloak.realm(realm).users().search(request.getUsername(), true);
                if (users.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
                }
                String userId = users.get(0).getId();
                
                List<String> roles = adminKeycloak.realm(realm).users().get(userId).roles()
                        .realmLevel().listAll().stream()
                        .map(RoleRepresentation::getName)
                        .collect(Collectors.toList());

                return AuthResponseDTO.builder()
                        .accessToken(tokenResponse.getToken())
                        .refreshToken(tokenResponse.getRefreshToken())
                        .tokenType(tokenResponse.getTokenType())
                        .expiresIn(tokenResponse.getExpiresIn())
                        .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                        .userId(userId)
                        .roles(roles)
                        .build();
            } catch (Exception e) {
                log.error("Ошибка при аутентификации пользователя", e);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверные учетные данные");
            }
        });
    }

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String lockKey = REFRESH_LOCK_PREFIX + request.getRefreshToken().hashCode();
        
        return lockUtil.executeWithLock(lockKey, () -> {
            try {
                // Создаем новый экземпляр клиента Keycloak для обновления токена
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm(realm)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .grantType("refresh_token")
                        .refreshToken(request.getRefreshToken())
                        .build();

                // Получаем новый токен
                AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();
                
                // Получаем информацию о пользователе из токена
                // Т.к. мы не можем напрямую получить userId из токена, используем поиск по имени пользователя
                // Для этого нам нужно было бы декодировать JWT токен, но в этом примере
                // мы просто возвращаем роли и токены без userId
                
                return AuthResponseDTO.builder()
                        .accessToken(tokenResponse.getToken())
                        .refreshToken(tokenResponse.getRefreshToken())
                        .tokenType(tokenResponse.getTokenType())
                        .expiresIn(tokenResponse.getExpiresIn())
                        .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                        .roles(Collections.emptyList()) // В реальном приложении нужно декодировать JWT и получить роли
                        .build();
            } catch (Exception e) {
                log.error("Ошибка при обновлении токена", e);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Недействительный refresh токен");
            }
        });
    }

    public AuthResponseDTO register(RegistrationRequestDTO request) {
        String lockKey = REGISTER_LOCK_PREFIX + request.getUsername();
        
        return lockUtil.executeWithLock(lockKey, () -> {
            try {
                UserRepresentation user = new UserRepresentation();
                user.setUsername(request.getUsername());
                user.setEmail(request.getEmail());
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setEmailVerified(false);
                user.setEnabled(true);

                Map<String, List<String>> attributes = new HashMap<>();
                if (request.getPhoneNumber() != null) {
                    attributes.put("phoneNumber", Collections.singletonList(request.getPhoneNumber()));
                }
                user.setAttributes(attributes);

                Response response = adminKeycloak.realm(realm).users().create(user);
                String userId = CreatedResponseUtil.getCreatedId(response);
                
                if (userId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка при создании пользователя");
                }

                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setTemporary(false);
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(request.getPassword());
                
                UserResource userResource = adminKeycloak.realm(realm).users().get(userId);
                userResource.resetPassword(passwordCred);

                RoleRepresentation userRole = adminKeycloak.realm(realm).roles().get("user").toRepresentation();
                userResource.roles().realmLevel().add(Collections.singletonList(userRole));

                AuthRequestDTO authRequest = new AuthRequestDTO(request.getUsername(), request.getPassword());
                return login(authRequest);
                
            } catch (Exception e) {
                log.error("Ошибка при регистрации пользователя", e);
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка при регистрации: " + e.getMessage());
            }
        });
    }
} 