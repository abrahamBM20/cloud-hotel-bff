package com.hotel.bff.reservations.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lo que el frontend recibe del BFF. Es el mismo JSON que devuelve la Lambda
 * 'reservation-query': el BFF no transforma los datos, solo los reenvía.
 */
public record ReservationResponseDto(
        String id,
        String guestName,
        String guestEmail,
        String roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer guests,
        BigDecimal totalAmount,
        String status,
        String notes,
        Instant requestedAt,
        Instant updatedAt
) {
}
