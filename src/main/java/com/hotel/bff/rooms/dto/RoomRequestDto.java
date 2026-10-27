package com.hotel.bff.rooms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RoomRequestDto(
        @NotBlank String roomNumber,
        @NotNull String roomType,
        @NotNull @Positive BigDecimal pricePerNight,
        Boolean isAvailable,
        String description,
        String imageUrl
) {
}
