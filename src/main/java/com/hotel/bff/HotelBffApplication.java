package com.hotel.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BFF (Backend for Frontend) del Hotel Boutique 360.
 *
 * Es la única puerta de entrada del frontend:
 *   - Catálogo de habitaciones  -> delega en el microservicio ms-hotel-rooms.
 *   - Reservas (lectura)       -> delega síncronamente en ms-hotel-rooms.
 *   - Reservas (escritura)     -> publica en SNS; la Lambda 'reservation-recorder' (SQS) las persiste.
 */
@SpringBootApplication
public class HotelBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelBffApplication.class, args);
    }

}
