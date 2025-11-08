package com.telecamnig.cinemapos;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.telecamnig.cinemapos.entity.Role;
import com.telecamnig.cinemapos.entity.User;
import com.telecamnig.cinemapos.repository.RoleRepository;
import com.telecamnig.cinemapos.repository.UserRepository;
import com.telecamnig.cinemapos.utility.Constants.UserRole;
import com.telecamnig.cinemapos.utility.Constants.UserStatus;

@Component
public class UserStartupDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.firstName}")
    private String adminFirstName;

    @Value("${admin.lastName}")
    private String adminLastName;

    @Value("${admin.emailId}")
    private String adminEmailId;

    @Value("${admin.contactNo}")
    private String adminContactNo;

    @Value("${admin.password}")
    private String adminPassword;

    public UserStartupDataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // ensure roles exist (seed from enum)
        if (!roleRepository.existsByName(UserRole.ROLE_ADMIN.value())) {
            roleRepository.save(Role.builder().name(UserRole.ROLE_ADMIN.value()).build());
        }

        if (!roleRepository.existsByName(UserRole.ROLE_COUNTER.value())) {
            roleRepository.save(Role.builder().name(UserRole.ROLE_COUNTER.value()).build());
        }

        // normalize admin email & contact
        String normalizedAdminEmail = adminEmailId == null ? "" : adminEmailId.trim().toLowerCase();
        String normalizedContact = adminContactNo == null ? null : adminContactNo.trim();

        // create admin user if not present
        if (!normalizedAdminEmail.isEmpty() && userRepository.findByEmailId(normalizedAdminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByName(UserRole.ROLE_ADMIN.value()).orElseThrow();

            User admin = User.builder()
                    .firstName(adminFirstName)
                    .lastName(adminLastName)
                    .emailId(normalizedAdminEmail)
                    .contactNo(normalizedContact)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(Set.of(adminRole))
                    .status(UserStatus.ACTIVE.getCode())
                    .build();

            userRepository.save(admin);
            System.out.println("Default admin created: " + normalizedAdminEmail);
        } else {
            System.out.println("Admin user already exists or admin email not configured: " + normalizedAdminEmail);
        }
    }
}
