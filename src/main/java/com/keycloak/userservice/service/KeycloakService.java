package com.keycloak.userservice.service;

import com.keycloak.userservice.dto.KeycloakUserDTO;
import com.keycloak.userservice.util.CreatedResponseUtil;
import com.keycloak.userservice.util.DistributedLockUtil;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KeycloakService {

    private final Keycloak keycloak;
    private final String realm;
    private final DistributedLockUtil lockUtil;

    private static final String CREATE_USER_LOCK_PREFIX = "lock:create-user:";
    private static final String UPDATE_USER_LOCK_PREFIX = "lock:update-user:";
    private static final String DELETE_USER_LOCK_PREFIX = "lock:delete-user:";
    private static final String GET_USER_LOCK_PREFIX = "lock:get-user:";
    private static final String GET_ALL_USERS_LOCK = "lock:get-all-users";

    @Autowired
    public KeycloakService(
            Keycloak keycloak, 
            @Value("${keycloak.realm}") String realm,
            DistributedLockUtil lockUtil) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.lockUtil = lockUtil;
    }

    public String createUser(KeycloakUserDTO userDTO) {
        String lockKey = CREATE_USER_LOCK_PREFIX + userDTO.getUsername();
        
        return lockUtil.executeWithLock(lockKey, () -> {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setEmailVerified(userDTO.getEmailVerified());
            
            Map<String, List<String>> attributes = new HashMap<>();
            
            if (userDTO.getPhoneNumber() != null) {
                attributes.put("phoneNumber", Collections.singletonList(userDTO.getPhoneNumber()));
            }
            
            if (userDTO.getPreferences() != null) {
                attributes.put("preferences", Collections.singletonList(userDTO.getPreferences()));
            }
            
            user.setAttributes(attributes);
            
            Response response = keycloak.realm(realm).users().create(user);
            String userId = CreatedResponseUtil.getCreatedId(response);
            
            if (userId != null) {
                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setTemporary(false);
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(userDTO.getUsername() + "123"); // Default password pattern
                
                UserResource userResource = keycloak.realm(realm).users().get(userId);
                userResource.resetPassword(passwordCred);

                if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                    List<RoleRepresentation> rolesToAdd = new ArrayList<>();
                    userDTO.getRoles().forEach(roleName -> {
                        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
                        rolesToAdd.add(role);
                    });
                    userResource.roles().realmLevel().add(rolesToAdd);
                }
            }
            
            return userId;
        });
    }

    public void updateUser(String userId, KeycloakUserDTO userDTO) {
        String lockKey = UPDATE_USER_LOCK_PREFIX + userId;
        
        lockUtil.executeWithLock(lockKey, () -> {
            UserRepresentation user = new UserRepresentation();
            user.setEmail(userDTO.getEmail());
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setEnabled(userDTO.getEnabled());
            user.setEmailVerified(userDTO.getEmailVerified());
            
            Map<String, List<String>> attributes = new HashMap<>();
            
            if (userDTO.getPhoneNumber() != null) {
                attributes.put("phoneNumber", Collections.singletonList(userDTO.getPhoneNumber()));
            }
            
            if (userDTO.getPreferences() != null) {
                attributes.put("preferences", Collections.singletonList(userDTO.getPreferences()));
            }
            
            user.setAttributes(attributes);
            
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            userResource.update(user);
            
            if (userDTO.getRoles() != null) {
                List<RoleRepresentation> allRoles = userResource.roles().realmLevel().listAll();
                userResource.roles().realmLevel().remove(allRoles);
                
                List<RoleRepresentation> newRoles = userDTO.getRoles().stream()
                        .map(roleName -> keycloak.realm(realm).roles().get(roleName).toRepresentation())
                        .toList();
                
                userResource.roles().realmLevel().add(newRoles);
            }
        });
    }

    public void deleteUser(String userId) {
        String lockKey = DELETE_USER_LOCK_PREFIX + userId;
        
        lockUtil.executeWithLock(lockKey, () -> {
            keycloak.realm(realm).users().delete(userId);
        });
    }

    public KeycloakUserDTO getUser(String userId) {
        String lockKey = GET_USER_LOCK_PREFIX + userId;
        
        return lockUtil.executeWithLock(lockKey, () -> {
            UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();
            KeycloakUserDTO userDTO = new KeycloakUserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setFirstName(user.getFirstName());
            userDTO.setLastName(user.getLastName());
            userDTO.setEnabled(user.isEnabled());
            userDTO.setEmailVerified(user.isEmailVerified());
            
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes != null) {
                if (attributes.containsKey("phoneNumber")) {
                    userDTO.setPhoneNumber(attributes.get("phoneNumber").get(0));
                }
                if (attributes.containsKey("preferences")) {
                    userDTO.setPreferences(attributes.get("preferences").get(0));
                }
            }
            
            List<String> roles = keycloak.realm(realm).users().get(userId).roles()
                    .realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .toList();
            
            userDTO.setRoles(roles);
            
            return userDTO;
        });
    }

    public List<KeycloakUserDTO> getAllUsers() {
        return lockUtil.executeWithLock(GET_ALL_USERS_LOCK, () -> {
            return keycloak.realm(realm).users().list().stream()
                    .map(user -> {
                        KeycloakUserDTO userDTO = new KeycloakUserDTO();
                        userDTO.setId(user.getId());
                        userDTO.setUsername(user.getUsername());
                        userDTO.setEmail(user.getEmail());
                        userDTO.setFirstName(user.getFirstName());
                        userDTO.setLastName(user.getLastName());
                        userDTO.setEnabled(user.isEnabled());
                        userDTO.setEmailVerified(user.isEmailVerified());
                        
                        Map<String, List<String>> attributes = user.getAttributes();
                        if (attributes != null) {
                            if (attributes.containsKey("phoneNumber")) {
                                userDTO.setPhoneNumber(attributes.get("phoneNumber").get(0));
                            }
                            if (attributes.containsKey("preferences")) {
                                userDTO.setPreferences(attributes.get("preferences").get(0));
                            }
                        }
                        
                        List<String> roles = keycloak.realm(realm).users().get(user.getId()).roles()
                                .realmLevel().listAll().stream()
                                .map(RoleRepresentation::getName)
                                .toList();
                        
                        userDTO.setRoles(roles);
                        
                        return userDTO;
                    })
                    .toList();
        });
    }
} 