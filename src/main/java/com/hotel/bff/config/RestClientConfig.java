package com.hotel.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Clientes HTTP de salida del BFF:
 *   - roomsClient: habla con el microservicio interno ms-hotel-rooms.
 *   - reservationsLambdaClient: invoca la Lambda 'reservation-query' por su Function URL.
 *
 * El BFF es la única capa pública de seguridad. ms-hotel-rooms solo es accesible
 * por la red interna de EC2 y no recibe secretos del navegador.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestClient roomsClient(@Value("${app.rooms.service-url}") String roomsServiceUrl) {
        return RestClient.builder()
                .baseUrl(roomsServiceUrl)
                .build();
    }

    @Bean
    public RestClient reservationsLambdaClient() {
        // La URL completa de invocación se aplica en cada request, por eso no lleva baseUrl.
        return RestClient.builder().build();
    }
}
