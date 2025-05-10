package com.keycloak.userservice.grpc;

import com.keycloak.userservice.dto.KeycloakUserDTO;
import com.keycloak.userservice.service.KeycloakService;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GrpcUserService extends UserServiceGrpc.UserServiceImplBase {

    private final KeycloakService keycloakService;

    @Autowired
    public GrpcUserService(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Override
    public void getMyProfile(GetMyProfileRequest request, StreamObserver<UserProfile> responseObserver) {
        try {
            KeycloakUserDTO user = keycloakService.getUser(request.getUserId());
            UserProfile profile = convertToGrpcUserProfile(user);
            responseObserver.onNext(profile);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateMyProfile(UpdateMyProfileRequest request, StreamObserver<Empty> responseObserver) {
        try {
            KeycloakUserDTO userDTO = convertFromGrpcUserProfile(request.getProfile());
            userDTO.setEnabled(true);
            keycloakService.updateUser(request.getUserId(), userDTO);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deactivateMyProfile(DeactivateMyProfileRequest request, StreamObserver<Empty> responseObserver) {
        try {
            KeycloakUserDTO userDTO = keycloakService.getUser(request.getUserId());
            userDTO.setEnabled(false);
            keycloakService.updateUser(request.getUserId(), userDTO);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAllUsers(GetAllUsersRequest request, StreamObserver<UserList> responseObserver) {
        try {
            List<KeycloakUserDTO> users = keycloakService.getAllUsers();
            UserList userList = UserList.newBuilder()
                    .addAllUsers(users.stream()
                            .map(this::convertToGrpcUserProfile)
                            .collect(Collectors.toList()))
                    .build();
            responseObserver.onNext(userList);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        try {
            KeycloakUserDTO userDTO = convertFromGrpcUserProfile(request.getProfile());
            String userId = keycloakService.createUser(userDTO);
            CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setUserId(userId)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<Empty> responseObserver) {
        try {
            KeycloakUserDTO userDTO = convertFromGrpcUserProfile(request.getProfile());
            keycloakService.updateUser(request.getUserId(), userDTO);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
        try {
            keycloakService.deleteUser(request.getUserId());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private UserProfile convertToGrpcUserProfile(KeycloakUserDTO user) {
        UserProfile.Builder builder = UserProfile.newBuilder()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setEnabled(user.getEnabled())
                .setEmailVerified(user.getEmailVerified())
                .addAllRoles(user.getRoles());

        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes != null) {
            attributes.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    builder.putAttributes(key, values.get(0));
                }
            });
        }

        if (user.getPhoneNumber() != null) {
            builder.setPhoneNumber(user.getPhoneNumber());
        }

        if (user.getPreferences() != null) {
            builder.setPreferences(user.getPreferences());
        }

        return builder.build();
    }

    private KeycloakUserDTO convertFromGrpcUserProfile(UserProfile profile) {
        KeycloakUserDTO userDTO = new KeycloakUserDTO();
        userDTO.setId(profile.getId());
        userDTO.setUsername(profile.getUsername());
        userDTO.setEmail(profile.getEmail());
        userDTO.setFirstName(profile.getFirstName());
        userDTO.setLastName(profile.getLastName());
        userDTO.setEnabled(profile.getEnabled());
        userDTO.setEmailVerified(profile.getEmailVerified());
        userDTO.setRoles(profile.getRolesList());

        Map<String, List<String>> attributes = profile.getAttributesMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.of(entry.getValue())
                ));
        userDTO.setAttributes(attributes);

        if (!profile.getPhoneNumber().isEmpty()) {
            userDTO.setPhoneNumber(profile.getPhoneNumber());
        }

        if (!profile.getPreferences().isEmpty()) {
            userDTO.setPreferences(profile.getPreferences());
        }

        return userDTO;
    }
} 