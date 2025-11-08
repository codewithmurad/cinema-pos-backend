package com.telecamnig.cinemapos.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.UserDto;
import com.telecamnig.cinemapos.dto.UserResponse;
import com.telecamnig.cinemapos.dto.UsersResponse;
import com.telecamnig.cinemapos.entity.Role;
import com.telecamnig.cinemapos.entity.User;
import com.telecamnig.cinemapos.repository.UserRepository;
import com.telecamnig.cinemapos.service.UserService;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.UserRole;
import com.telecamnig.cinemapos.utility.Constants.UserStatus;

import jakarta.transaction.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // reuse existing constructor if present
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public ResponseEntity<UserResponse> getUserByPublicId(String publicUserId) {
        if (publicUserId == null || publicUserId.isBlank()) {
            UserResponse bad = UserResponse.builder()
                    .success(false)
                    .message(ApiResponseMessage.INVALID_INPUT)
                    .build();
            return ResponseEntity.badRequest().body(bad);
        }

        return userRepository.findByUserId(publicUserId.trim())
                .map(this::toUserDtoResponse)
                .orElseGet(() -> {
                    UserResponse notFound = UserResponse.builder()
                            .success(false)
                            .message(ApiResponseMessage.USER_NOT_FOUND)
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
                });
    }

    @Override
    public ResponseEntity<UsersResponse> getUsersByRoleAndStatus(String roleParam, Integer status) {
        if (roleParam == null || roleParam.isBlank()) {
            UsersResponse bad = UsersResponse.builder()
                    .success(false)
                    .message(ApiResponseMessage.INVALID_INPUT)
                    .build();
            return ResponseEntity.badRequest().body(bad);
        }

        // normalize role: accept "ADMIN" or "ROLE_ADMIN"
        String roleNormalized = roleParam.trim().toUpperCase();
        if (!roleNormalized.startsWith("ROLE_")) {
            roleNormalized = "ROLE_" + roleNormalized;
        }

        // validate role against known enum constants
        if (!UserRole.isValidRole(roleNormalized)) {
            UsersResponse bad = UsersResponse.builder()
                    .success(false)
                    .message("Invalid role: " + roleParam)
                    .build();
            return ResponseEntity.badRequest().body(bad);
        }

        List<User> users;
        if (status == null) {
            users = userRepository.findByRoleName(roleNormalized);
        } else {
            users = userRepository.findByRoleNameAndStatus(roleNormalized, status);
        }

        List<UserDto> dtoList = users.stream().map(this::toUserDto).collect(Collectors.toList());

        String message = dtoList.isEmpty() ? ApiResponseMessage.NO_DATA_FOUND : ApiResponseMessage.FETCH_SUCCESS;

        UsersResponse resp = UsersResponse.builder()
                .success(true)
                .message(message)
                .users(dtoList)
                .build();

        return ResponseEntity.ok(resp);
    }
    
    @Override
    @Transactional
    public ResponseEntity<CommonApiResponse> updateUserStatus(String publicUserId, Integer status) {

        if (publicUserId == null || publicUserId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, ApiResponseMessage.INVALID_INPUT));
        }

        if (status == null || !UserStatus.isValidStatusCode(status)) {
            return ResponseEntity.badRequest()
                    .body(new CommonApiResponse(false, ApiResponseMessage.INVALID_USER_STATUS_CODE));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
        }

        String loggedInEmail = authentication.getName(); // since our UserDetailsService uses email
        Optional<User> loggedInUserOpt = userRepository.findByEmailId(loggedInEmail);

        if (loggedInUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
        }

        User loggedInUser = loggedInUserOpt.get();

        boolean isAdmin = loggedInUser.getRoles().stream()
                .anyMatch(role -> UserRole.ROLE_ADMIN.value().equalsIgnoreCase(role.getName()));

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonApiResponse(false, ApiResponseMessage.ACCESS_DENIED));
        }

        Optional<User> targetUserOpt = userRepository.findByUserId(publicUserId.trim());
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonApiResponse(false, ApiResponseMessage.USER_NOT_FOUND));
        }

        User targetUser = targetUserOpt.get();

        if (loggedInUser.getUserId().equals(targetUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.SELF_STATUS_CHANGE_NOT_ALLOWED));
        }

        targetUser.setStatus(status);
        userRepository.save(targetUser);

        return ResponseEntity.ok(new CommonApiResponse(true, ApiResponseMessage.USER_STATUS_UPDATED));
    }


    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailId(user.getEmailId())
                .contactNo(user.getContactNo())
                .status(user.getStatus())
                .roles(user.getRoles() == null ? null :
                        user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    private ResponseEntity<UserResponse> toUserDtoResponse(User user) {
        UserDto dto = UserDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailId(user.getEmailId())
                .contactNo(user.getContactNo())
                .status(user.getStatus())
                .roles(user.getRoles() == null ? null :
                        user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        UserResponse resp = UserResponse.builder()
                .success(true)
                .message(ApiResponseMessage.FETCH_SUCCESS)
                .user(dto)
                .build();

        return ResponseEntity.ok(resp);
    }

}
