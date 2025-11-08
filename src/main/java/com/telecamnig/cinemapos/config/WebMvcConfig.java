package com.telecamnig.cinemapos.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.telecamnig.cinemapos.interceptor.RequestResponseLoggingInterceptor;

/**
 * Registers interceptors and CORS configuration for all incoming requests.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestResponseLoggingInterceptor loggingInterceptor;

    @Autowired
    public WebMvcConfig(RequestResponseLoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**") // Intercept all paths
                .excludePathPatterns(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/webjars/**",
                    "/swagger-resources/**",
                    "/configuration/ui",
                    "/configuration/security",
                    "/error"
                ); // Exclude Swagger endpoints if needed
    }

    /**
     * CORS configuration for React frontend
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                    "http://localhost:3000",    // React development server
                    "http://127.0.0.1:3000",    // Alternative localhost
                    "http://localhost:5173",    // Vite development server
                    "http://127.0.0.1:5173"     // Vite alternative
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 1 hour
    }
}