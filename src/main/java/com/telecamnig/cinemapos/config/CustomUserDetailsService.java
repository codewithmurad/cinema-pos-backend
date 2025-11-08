package com.telecamnig.cinemapos.config;

import com.telecamnig.cinemapos.entity.User;
import com.telecamnig.cinemapos.repository.UserRepository;
import com.telecamnig.cinemapos.utility.ApiResponseMessage;
import com.telecamnig.cinemapos.utility.Constants.UserStatus;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.trim().isEmpty()) {
            throw new UsernameNotFoundException(ApiResponseMessage.EMAIL_REQUIRED);
        }

        // normalize email for consistent lookup
        String normalizedEmail = email.trim().toLowerCase();

        // fetch user by email only
        User user = userRepository.findByEmailId(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException(ApiResponseMessage.EMAIL_NOT_FOUND));

        // convert Role entity -> GrantedAuthority
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))  // roles in DB as ROLE_ADMIN / ROLE_COUNTER
                .collect(Collectors.toList());

        boolean enabled = user.getStatus() == UserStatus.ACTIVE.getCode();

        return new org.springframework.security.core.userdetails.User(
                user.getEmailId(),
                user.getPassword(),
                enabled,
                true,   // accountNonExpired
                true,   // credentialsNonExpired
                true,   // accountNonLocked
                authorities
        );
    }

}
