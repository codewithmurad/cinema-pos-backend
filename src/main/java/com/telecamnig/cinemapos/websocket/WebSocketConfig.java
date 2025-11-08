package com.telecamnig.cinemapos.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket configuration for real-time seat booking synchronization across all POS systems.
 * 
 * This configuration enables STOMP over WebSocket for bidirectional communication between
 * the backend and multiple POS clients (React Electron apps).
 * 
 * FEATURES:
 * - Real-time seat state updates (AVAILABLE/HELD/SOLD)
 * - Live booking notifications
 * - Multi-POS system synchronization
 * - Automatic reconnection support
 * 
 * @author Your Name
 * @version 1.0
 */

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker for WebSocket communication.
     * 
     * BROKER CHANNELS:
     * - /topic: For broadcasting messages to multiple subscribers (seat updates, bookings)
     * - /user: For user-specific messages (personal notifications)
     * - /app: For application destination prefix (client-to-server messages)
     * 
     * @param registry The MessageBrokerRegistry to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        
    	// Enable a simple in-memory message broker to carry messages back to clients
        // In production, consider using a dedicated message broker like RabbitMQ or ActiveMQ
        registry.enableSimpleBroker("/topic", "/user");
        
        // Designate the "/app" prefix for messages that are bound for @MessageMapping methods
        // Clients send messages to /app/xxx which routes to corresponding @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages (optional, for direct user messaging)
        registry.setUserDestinationPrefix("/user");
    
    }

    /**
     * Registers STOMP endpoints for WebSocket connections.
     * 
     * ENDPOINTS:
     * - /ws: Main WebSocket endpoint for POS clients
     * - SockJS fallback for browsers that don't support WebSocket
     * - CORS configuration for cross-origin requests
     * 
     * @param registry The StompEndpointRegistry to configure
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint for WebSocket connections
        // This is the endpoint that POS clients will connect to
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // In production, restrict to specific origins
                .withSockJS(); // Enable SockJS fallback options for browsers that don't support WebSocket
        
        // Additional endpoint without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        
        log.info("WebSocket STOMP endpoints registered: /ws (with SockJS fallback)");
    }

}