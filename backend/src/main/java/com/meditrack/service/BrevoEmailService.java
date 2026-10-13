package com.meditrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional alert emails (expiry risk, transfer suggestions)
 * through Brevo's HTTP API (https://api.brevo.com/v3/smtp/email).
 *
 * Requires BREVO_API_KEY and EMAIL_FROM to be set as environment
 * variables. If BREVO_API_KEY is not configured, sends are skipped and
 * logged rather than failing the request that triggered them - an alert
 * email failing to send should never break a dispense or dashboard load.
 */
@Service
public class BrevoEmailService {

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailService(@Value("${meditrack.email.brevo-api-key:}") String apiKey,
                              @Value("${meditrack.email.from-address:}") String fromEmail,
                              @Value("${meditrack.email.from-name:MediTrack}") String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }

    public void send(String toEmail, String toName, String subject, String htmlContent) {
        if (!isConfigured()) {
            System.out.println("[MediTrack] Email alert skipped (Brevo not configured): " + subject);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);
        headers.set("accept", "application/json");

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", toEmail, "name", toName)),
                "subject", subject,
                "htmlContent", htmlContent
        );

        try {
            restTemplate.postForEntity(BREVO_ENDPOINT, new HttpEntity<>(body, headers), String.class);
            System.out.println("[MediTrack] Alert email sent to " + toEmail + ": " + subject);
        } catch (Exception e) {
            // Never let an email failure break the request that triggered it.
            System.err.println("[MediTrack] Failed to send alert email: " + e.getMessage());
        }
    }
}
