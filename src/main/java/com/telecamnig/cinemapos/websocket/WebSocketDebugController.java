package com.telecamnig.cinemapos.websocket;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telecamnig.cinemapos.dto.SeatStateEvent;

@RestController
@RequestMapping("/api/debug")
public class WebSocketDebugController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private WebSocketService webSocketService;

    /**
     * Test WebSocket connection and message flow
     */
    @GetMapping("/websocket/test")
    public String testWebSocket() {
        // Send test message to seat topic
        SeatStateEvent testEvent = SeatStateEvent.builder()
                .showPublicId("test-show-123")
                .seatPublicId("test-seat-1")
                .state("TEST")
                .reservedBy("debug-user")
                .timestamp(LocalDateTime.now())
                .eventType("DEBUG_TEST")
                .build();
        
        messagingTemplate.convertAndSend("/topic/shows/test-show-123/seats", testEvent);
        
        return "WebSocket test message sent! Check logs for message flow.";
    }
    
    /**
     * Get active WebSocket sessions info
     */
    @GetMapping("/websocket/sessions")
    public Map<String, Object> getActiveSessions() {
        // This would require custom session tracking
        return Map.of(
            "status", "WebSocket Active",
            "backend", "In-Memory Broker",
            "message", "Check logs for detailed session info"
        );
    }
}