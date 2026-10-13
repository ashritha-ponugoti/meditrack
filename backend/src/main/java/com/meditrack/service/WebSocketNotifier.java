package com.meditrack.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends a lightweight "something changed" signal to any client subscribed
 * to a pharmacy's topic. Clients react by re-fetching the relevant data,
 * rather than the server pushing full entity state over the socket -
 * keeps this simple and avoids duplicating serialization logic.
 */
@Service
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyPharmacy(Long pharmacyId, String eventType) {
        messagingTemplate.convertAndSend(
                "/topic/pharmacy/" + pharmacyId,
                Map.of("event", eventType, "timestamp", System.currentTimeMillis())
        );
    }
}
