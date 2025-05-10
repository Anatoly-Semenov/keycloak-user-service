package com.keycloak.userservice.service;

import com.keycloak.userservice.dto.KeycloakUserDTO;
import com.keycloak.userservice.event.UserEventType;
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
    private final UserEventService userEventService;

    private static final String CREATE_USER_LOCK_PREFIX = "lock:create-user:";
    private static final String UPDATE_USER_LOCK_PREFIX = "lock:update-user:";
    private static final String DELETE_USER_LOCK_PREFIX = "lock:delete-user:";
    private static final String GET_USER_LOCK_PREFIX = "lock:get-user:";
    private static final String GET_ALL_USERS_LOCK = "lock:get-all-users";

    @Autowired
    public KeycloakService(
            Keycloak keycloak, 
            @Value("${keycloak.realm}") String realm,
            DistributedLockUtil lockUtil,
            UserEventService userEventService) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.lockUtil = lockUtil;
        this.userEventService = userEventService;
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
                passwordCred.setValue(userDTO.getUsername() + "123");
                
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
            UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();
            

            Map<String, Object> oldValues = new HashMap<>();
            oldValues.put("email", user.getEmail());
            oldValues.put("firstName", user.getFirstName());
            oldValues.put("lastName", user.getLastName());
            oldValues.put("enabled", user.isEnabled());
            oldValues.put("emailVerified", user.isEmailVerified());
            oldValues.put("attributes", user.getAttributes());
            
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
            
            keycloak.realm(realm).users().get(userId).update(user);


            Map<String, Object> changes = new HashMap<>();
            if (!Objects.equals(oldValues.get("email"), userDTO.getEmail())) {
                changes.put("email", userDTO.getEmail());
            }
            if (!Objects.equals(oldValues.get("firstName"), userDTO.getFirstName())) {
                changes.put("firstName", userDTO.getFirstName());
            }
            if (!Objects.equals(oldValues.get("lastName"), userDTO.getLastName())) {
                changes.put("lastName", userDTO.getLastName());
            }
            if (!Objects.equals(oldValues.get("enabled"), userDTO.getEnabled())) {
                changes.put("enabled", userDTO.getEnabled());
            }
            if (!Objects.equals(oldValues.get("emailVerified"), userDTO.getEmailVerified())) {
                changes.put("emailVerified", userDTO.getEmailVerified());
            }
            if (!Objects.equals(oldValues.get("attributes"), attributes)) {
                changes.put("attributes", attributes);
            }

            userEventService.sendUserEvent(UserEventType.PROFILE_UPDATED, userId, changes);
        });
    }

    public void deleteUser(String userId) {
        String lockKey = DELETE_USER_LOCK_PREFIX + userId;
        
        lockUtil.executeWithLock(lockKey, () -> {

            UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();
            
            keycloak.realm(realm).users().delete(userId);

            userEventService.sendUserEvent(UserEventType.USER_DELETED, userId, 
                Map.of("username", user.getUsername(), "email", user.getEmail()));
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

            userEventService.sendUserEvent(UserEventType.PROFILE_VIEWED, userId, 
                Map.of("viewedBy", "SYSTEM"));
            
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

    public void updateUserRoles(String userId, List<String> roles) {
        String lockKey = UPDATE_USER_LOCK_PREFIX + userId;
        
        lockUtil.executeWithLock(lockKey, () -> {

            List<String> currentRoles = keycloak.realm(realm).users().get(userId).roles()
                    .realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .toList();
            

            List<String> rolesToAdd = roles.stream()
                    .filter(role -> !currentRoles.contains(role))
                    .toList();
            
            List<String> rolesToRemove = currentRoles.stream()
                    .filter(role -> !roles.contains(role))
                    .toList();
            

            if (!rolesToAdd.isEmpty()) {
                List<RoleRepresentation> rolesToAddRep = rolesToAdd.stream()
                        .map(roleName -> keycloak.realm(realm).roles().get(roleName).toRepresentation())
                        .toList();
                keycloak.realm(realm).users().get(userId).roles().realmLevel().add(rolesToAddRep);
                
                userEventService.sendUserEvent(UserEventType.ROLE_ASSIGNED, userId, 
                    Map.of("roles", rolesToAdd));
            }
            

            if (!rolesToRemove.isEmpty()) {
                List<RoleRepresentation> rolesToRemoveRep = rolesToRemove.stream()
                        .map(roleName -> keycloak.realm(realm).roles().get(roleName).toRepresentation())
                        .toList();
                keycloak.realm(realm).users().get(userId).roles().realmLevel().remove(rolesToRemoveRep);
                
                userEventService.sendUserEvent(UserEventType.ROLE_REMOVED, userId, 
                    Map.of("roles", rolesToRemove));
            }
        });
    }
} 