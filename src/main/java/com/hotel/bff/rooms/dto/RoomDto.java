package com.hotel.bff.rooms.dto;

import java.math.BigDecimal;

/**
 * Copia del JSON que devuelve el microservicio ms-hotel-rooms.
 * El BFF no persiste habitaciones: solo las agrega y las entrega al frontend.
 */
public record RoomDto(
        String id,
        String roomNumber,
        String roomType,
        BigDecimal pricePerNight,
        Boolean isAvailable,
        String description,
        String imageUrl
) {
}
