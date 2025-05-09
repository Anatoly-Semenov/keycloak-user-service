package com.keycloak.userservice.controller;

import com.keycloak.userservice.dto.KeycloakUserDTO;
import com.keycloak.userservice.service.KeycloakService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final KeycloakService keycloakService;

    @Autowired
    public UserController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('user')")
    public ResponseEntity<KeycloakUserDTO> getMyProfile(@RequestHeader("X-User-ID") String userId) {
        return ResponseEntity.ok(keycloakService.getUser(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('user')")
    public ResponseEntity<Void> updateMyProfile(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody KeycloakUserDTO userDTO) {
        userDTO.setEnabled(true);
        keycloakService.updateUser(userId, userDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('user')")
    public ResponseEntity<Void> deactivateMyProfile(@RequestHeader("X-User-ID") String userId) {
        KeycloakUserDTO userDTO = keycloakService.getUser(userId);
        userDTO.setEnabled(false);
        keycloakService.updateUser(userId, userDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<KeycloakUserDTO>> getAllUsers() {
        return ResponseEntity.ok(keycloakService.getAllUsers());
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> createUser(@Valid @RequestBody KeycloakUserDTO userDTO) {
        String userId = keycloakService.createUser(userDTO);
        return ResponseEntity.ok(userId);
    }

    @PutMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody KeycloakUserDTO userDTO) {
        keycloakService.updateUser(userId, userDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        keycloakService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
} 