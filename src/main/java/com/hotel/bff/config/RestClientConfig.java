package com.hotel.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP de salida del BFF:
 *   - roomsClient: habla con el microservicio ms-hotel-rooms.
 *   - reservationsLambdaClient: invoca la Lambda 'reservation-query' por su Function URL.
 *
 * El secreto de la pasarela protege SOLO el tramo BFF -> ms-hotel-rooms: es la
 * capa añadida entre el API Gateway y el microservicio. La Lambda se invoca por
 * su Function URL, que va con --auth-type NONE, así que no lleva secreto.
 *
 * El BFF NUNCA reenvía la cabecera que recibe del cliente: reenvía la suya
 * (app.gateway.secret / app.rooms.gateway.secret). Si reenviara la del
 * cliente, cualquiera que llamara al BFF con un secreto adivinado o inyectado
 * por el API Gateway podría suplantar al gateway frente a ms-hotel-rooms.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient roomsClient(@Value("${app.rooms.service-url}") String roomsServiceUrl,
                                  @Value("${app.rooms.gateway.secret}") String roomsGatewaySecret) {
        return RestClient.builder()
                .baseUrl(roomsServiceUrl)
                .defaultHeader("X-Secret-Gateway", roomsGatewaySecret)
                .build();
    }

    @Bean
    public RestClient reservationsLambdaClient() {
        // La URL completa de invocación se aplica en cada request, por eso no lleva baseUrl.
        return RestClient.builder().build();
    }
}
