package com.gestaofacil.service;

import com.gestaofacil.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class ResendEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String from;

    public ResendEmailService(
            RestClient.Builder builder,
            @Value("${app.email.resend-api-key:}") String apiKey,
            @Value("${app.email.from:}") String from
    ) {
        this.restClient = builder.baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey;
        this.from = from;
    }

    public void send(String recipient, String subject, String html) {
        if (apiKey.isBlank() || from.isBlank()) {
            throw new EmailDeliveryException("O envio de e-mail não está configurado. Defina RESEND_API_KEY e MAIL_FROM.");
        }
        try {
            restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(new ResendRequest(from, List.of(recipient), subject, html))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException("Não foi possível enviar o e-mail. Tente novamente mais tarde.", exception);
        }
    }

    private record ResendRequest(String from, List<String> to, String subject, String html) {}
}
