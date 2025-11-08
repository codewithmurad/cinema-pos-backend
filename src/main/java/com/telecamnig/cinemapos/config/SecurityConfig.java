package com.telecamnig.cinemapos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.telecamnig.cinemapos.filter.JwtAuthenticationFilter;
import com.telecamnig.cinemapos.utility.JwtUtil;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    
    private final JwtUtil jwtUtil;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
            		// Public endpoints - NO authentication required
//                    .requestMatchers(
//                    	"/api/**",       
//                        "/api/v1/auth/**",           // Authentication endpoints
//                        "/api/v1/movie/**", 
//                        "/api/screens/**",        // Screen endpoints
//                        "/actuator/health",       // Health check
//                        
//                        // Swagger/OpenAPI documentation - ADD THESE
//                        "/swagger-ui.html",
//                        "/swagger-ui/**",
//                        "/v3/api-docs/**",
//                        "/swagger-resources/**",
//                        "/swagger-resources",
//                        "/configuration/ui",
//                        "/configuration/security",
//                        "/webjars/**",
//                        "/api-docs/**",
//                        
//                        // WebSocket endpoints
//                        "/ws/**",
//                        "/ws",
//                        "/cinemapos/ws/**",
//                        "/cinemapos/ws",
//                        "/ws/info**",
//                        
//                        // Error endpoints
//                        "/error",
//                        "/error**"
//                    ).permitAll()
//                    
//                    
//            	    .anyRequest().authenticated()
            		.anyRequest().permitAll()
            	)

            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // production-safe
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
