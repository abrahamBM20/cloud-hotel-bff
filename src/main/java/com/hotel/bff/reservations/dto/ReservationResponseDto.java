package com.hotel.bff.reservations.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lo que el frontend recibe del BFF. El BFF conserva el contrato y delega
 * la lectura síncrona a ms-hotel-rooms.
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
