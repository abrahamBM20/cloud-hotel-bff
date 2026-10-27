package com.hotel.bff.reservations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.bff.reservations.dto.ReservationResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Invoca la Lambda 'reservation-query' por su Function URL y devuelve el JSON que ella entrega.
 *
 * El evento que se le manda es el formato AWS_PROXY:
 *   {"httpMethod":"GET","path":"/reservations","queryStringParameters":{...}}
 * y la Lambda responde:
 *   {"statusCode":200,"headers":{...},"body":"[ ... ]"}
 */
@Component
public class ReservationLambdaClient {

    private final RestClient lambdaClient;
    private final String lambdaUrl;
    private final String queryToken;
    private final ObjectMapper objectMapper;

    public ReservationLambdaClient(RestClient reservationsLambdaClient,
                                   @Value("${app.lambda.reservations.url}") String lambdaUrl,
                                   @Value("${app.lambda.query-token}") String queryToken,
                                   ObjectMapper objectMapper) {
        this.lambdaClient = reservationsLambdaClient;
        this.lambdaUrl = lambdaUrl;
        this.queryToken = queryToken;
        this.objectMapper = objectMapper;
    }

    /**
     * Pide las reservas a la Lambda. Los filtros opcionales se pasan como query string.
     * @param filters status, roomNumber, guestId, id, limit... (los nulos se omiten)
     */
    public List<ReservationResponseDto> findReservations(Map<String, String> filters) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("httpMethod", "GET");
        event.put("path", "/reservations");
        event.put("queryStringParameters", filters);
        event.put("isBase64Encoded", false);

        String rawResponse = lambdaClient.post()
                .uri(lambdaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Query-Token", queryToken)
                .body(event)
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La Lambda de reservas no devolvió respuesta");
        }

        try {
            JsonNode response = objectMapper.readTree(rawResponse);
            String body = rawResponse;

            // Una Function URL devuelve directamente el body de la Lambda.
            // Una invocación directa puede devolver la envoltura AWS_PROXY.
            if (response.isObject() && response.has("statusCode")) {
                int statusCode = response.path("statusCode").asInt(500);
                if (statusCode < 200 || statusCode >= 300) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "La Lambda de reservas respondió HTTP " + statusCode
                                    + ": " + response.path("body").asText());
                }
                body = response.path("body").asText("[]");
            }

            return objectMapper.readValue(body,
                    new TypeReference<List<ReservationResponseDto>>() {
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo interpretar el JSON de la Lambda de reservas", e);
        }
    }
}
