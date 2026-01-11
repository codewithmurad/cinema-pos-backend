package com.telecamnig.cinemapos.service.impl;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.telecamnig.cinemapos.dto.AdminRegisterRequest;
import com.telecamnig.cinemapos.dto.ChangePasswordRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.CounterRegisterRequest;
import com.telecamnig.cinemapos.dto.ForgotPasswordRequest;
import com.telecamnig.cinemapos.dto.LoginRequest;
import com.telecamnig.cinemapos.dto.LoginResponse;
import com.telecamnig.cinemapos.dto.UserDto;
import com.telecamnig.cinemapos.dto.UserResponse;
import com.telecamnig.cinemapos.entity.Role;
import com.telecamnig.cinemapos.entity.User;
import com.telecamnig.cinemapos.exception.InvalidCredentialsException;
import com.telecamnig.cinemapos.exception.UserAlreadyExistsException;
import com.telecamnig.cinemapos.repository.RoleRepository;
import com.telecamnig.cinemapos.repository.UserRepository;
import com.telecamnig.cinemapos.service.AuthService;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.UserRole;
import com.telecamnig.cinemapos.utility.Constants.UserStatus;
import com.telecamnig.cinemapos.utility.JwtUtil;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    
    private final UserRepository userRepository;
    
    private final RoleRepository roleRepository;
    
    private final PasswordEncoder passwordEncoder;
    
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
       
    	// normalize and validate email
        String email = (request.getEmail() == null) ? "" : request.getEmail().trim().toLowerCase();
        
        if (email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.builder()
                            .success(false)
                            .message(ApiResponseMessage.EMAIL_REQUIRED)
                            .build());
        }

        // authenticate user
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (Exception ex) {
            throw new InvalidCredentialsException(ApiResponseMessage.INVALID_CREDENTIALS);
        }

        // fetch user
        User user = userRepository.findByEmailId(email)
                .orElseThrow(() -> new InvalidCredentialsException(ApiResponseMessage.INVALID_CREDENTIALS));
        
        // In login method, after fetching user:
        if (user.getStatus() != UserStatus.ACTIVE.getCode()) {
            throw new InvalidCredentialsException("Account is inactive. Please contact administrator.");
        }

        var roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // generate token
        String token = jwtUtil.generateToken(user.getEmailId(), roles);
        
        long expiresIn = jwtUtil.getExpirationInSeconds();

        UserDto userDto = toUserDtoResponse(user);
        
        // build and return LoginResponse
        LoginResponse response = LoginResponse.builder()
                .success(true)
                .message(ApiResponseMessage.LOGIN_SUCCESS)
                .token(token)
                .expiresIn(expiresIn)
                .user(userDto)
                .build();

        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<CommonApiResponse> registerAdmin(AdminRegisterRequest request) {
        
    	String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim().toLowerCase() : "";
        
    	if (email.isEmpty()) {
            // Should normally be caught by DTO validation; defensive
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.EMAIL_REQUIRED));
        }
    	
    	if (!isPasswordStrong(request.getPassword())) {
    	    return ResponseEntity.badRequest()
    	            .body(new CommonApiResponse(false, "Password must be at least 8 characters with mix of letters and numbers"));
    	}

        if (userRepository.existsByEmailId(email)) {
            throw new UserAlreadyExistsException(ApiResponseMessage.EMAIL_ALREADY_EXISTS);
        }

        Role adminRole = roleRepository.findByName(UserRole.ROLE_ADMIN.value())
                .orElseThrow(() -> new IllegalStateException(ApiResponseMessage.ROLE_NOT_FOUND));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailId(email)
                .contactNo(request.getContact())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(adminRole))
                .status(UserStatus.ACTIVE.getCode())
                .build();

        userRepository.save(user);

        CommonApiResponse resp = new CommonApiResponse(true, ApiResponseMessage.ADMIN_REGISTER_SUCCESS);
    
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    
    }

    @Override
    public ResponseEntity<CommonApiResponse> registerCounter(CounterRegisterRequest request) {
        
    	String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim().toLowerCase() : "";
        
    	if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.EMAIL_REQUIRED));
        }
    	
    	if (!isPasswordStrong(request.getPassword())) {
    	    return ResponseEntity.badRequest()
    	            .body(new CommonApiResponse(false, "Password must be at least 8 characters with mix of letters and numbers"));
    	}

        if (userRepository.existsByEmailId(email)) {
            throw new UserAlreadyExistsException(ApiResponseMessage.EMAIL_ALREADY_EXISTS);
        }

        Role counterRole = roleRepository.findByName(UserRole.ROLE_COUNTER.value())
                .orElseThrow(() -> new IllegalStateException(ApiResponseMessage.ROLE_NOT_FOUND));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailId(email)
                .contactNo(request.getContact())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(counterRole))
                .status(UserStatus.ACTIVE.getCode())
                .build();

        userRepository.save(user);

        CommonApiResponse resp = new CommonApiResponse(true, ApiResponseMessage.COUNTER_REGISTER_SUCCESS);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    
    }
    
    @Override
    public ResponseEntity<CommonApiResponse> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByEmailId(email);
        if (existingUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.EMAIL_NOT_FOUND));
        }

        User user = existingUserOpt.get();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.PASSWORD_MISMATCH));
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return ResponseEntity.ok(new CommonApiResponse(true, ApiResponseMessage.PASSWORD_CHANGE_SUCCESS));
    }
    
    @Override
    public ResponseEntity<CommonApiResponse> changePassword(ChangePasswordRequest request) {
        
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
    	if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonApiResponse(false, ApiResponseMessage.UNAUTHORIZED_ACCESS));
        }

        String email = authentication.getName();
        
        User user = userRepository.findByEmailId(email)
                .orElseThrow(() -> new UsernameNotFoundException(ApiResponseMessage.EMAIL_NOT_FOUND));

        // 1. Validate old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.PASSWORD_OLD_INCORRECT));
        }

        // 2. Validate new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.PASSWORD_MISMATCH));
        }

        // 3. Prevent same password reuse
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonApiResponse(false, ApiResponseMessage.PASSWORD_SAME_AS_OLD));
        }

        // 4. Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new CommonApiResponse(true, ApiResponseMessage.PASSWORD_CHANGE_SUCCESS));
    }
    
    private boolean isPasswordStrong(String password) {
        return password != null && 
               password.length() >= 8 && 
               password.matches(".*[A-Za-z].*") && 
               password.matches(".*[0-9].*");
    }
    
    private UserDto toUserDtoResponse(User user) {
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

       return dto;
    }

}
