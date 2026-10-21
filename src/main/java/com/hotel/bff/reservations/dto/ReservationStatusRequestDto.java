package com.hotel.bff.reservations.dto;

import jakarta.validation.constraints.NotBlank;

public record ReservationStatusRequestDto(
        @NotBlank String status,
        String notes
) {
}
