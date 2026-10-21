package com.hotel.bff.reservations.dto;

import java.time.Instant;

/**
 * Respuesta del cambio de estado. El cambio es asíncrono (SNS -> SQS -> Lambda),
 * así que el BFF devuelve 202 Accepted con el estado pedido, no el estado ya aplicado.
 */
public record ReservationStatusResponseDto(
        String id,
        String status,
        String message,
        Instant changedAt
) {
}
