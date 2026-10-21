package com.hotel.bff.reservations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReservationRequestDto(
        @NotBlank String guestName,
        String guestEmail,
        @NotBlank String roomNumber,
        LocalDate checkIn,
        LocalDate checkOut,
        @Positive Integer guests,
        String notes
) {
}
