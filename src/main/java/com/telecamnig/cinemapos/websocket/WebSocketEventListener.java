package com.telecamnig.cinemapos.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SeatHoldService seatHoldService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("WebSocket connected - Session: {}, User: {}", sessionId, user);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("WebSocket disconnected - Session: {}, User: {}", sessionId, user);
        
        // 🚨 CRITICAL: Release all seats held by this session
        seatHoldService.releaseSeatsBySession(sessionId);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String user = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("WebSocket subscription - Session: {}, User: {}, Destination: {}", 
                sessionId, user, destination);
    }

}