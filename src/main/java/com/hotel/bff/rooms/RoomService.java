package com.hotel.bff.rooms;

import com.hotel.bff.rooms.dto.RoomDto;
import com.hotel.bff.reservations.dto.ReservationResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.hotel.bff.rooms.dto.RoomRequestDto;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Agregador del catálogo de habitaciones: el BFF delega en ms-hotel-rooms
 * y devuelve su JSON tal cual, sin tocar la base de datos de habitaciones.
 */
@Service
public class RoomService {

    private final RestClient roomsClient;

    public RoomService(RestClient roomsClient) {
        this.roomsClient = roomsClient;
    }

    public List<RoomDto> getAll() {
        RoomDto[] rooms = roomsClient.get()
                .uri("/api/v1/rooms")
                .retrieve()
                .body(RoomDto[].class);
        return rooms == null ? List.of() : Arrays.asList(rooms);
    }

    public Optional<RoomDto> getById(String id) {
        try {
            RoomDto room = roomsClient.get()
                    .uri("/api/v1/rooms/{id}", id)
                    .retrieve()
                    .body(RoomDto.class);
            return Optional.ofNullable(room);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /** Busca una habitación por su número. Lo usa la validación de reservas. */
    public Optional<RoomDto> findByRoomNumber(String roomNumber) {
        return getAll().stream()
                .filter(room -> room.roomNumber() != null
                        && room.roomNumber().equalsIgnoreCase(roomNumber.trim()))
                .findFirst();
    }

    public List<ReservationResponseDto> getReservations(String status, String roomNumber,
                                                        String guestId, String id, Integer limit) {
        ReservationResponseDto[] reservations = roomsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/reservations")
                        .queryParamIfPresent("status", Optional.ofNullable(status))
                        .queryParamIfPresent("roomNumber", Optional.ofNullable(roomNumber))
                        .queryParamIfPresent("guestId", Optional.ofNullable(guestId))
                        .queryParamIfPresent("id", Optional.ofNullable(id))
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build())
                .retrieve()
                .body(ReservationResponseDto[].class);
        return reservations == null ? List.of() : Arrays.asList(reservations);
    }

    public RoomDto create(RoomRequestDto request) {
        return roomsClient.post()
                .uri("/api/v1/rooms")
                .body(request)
                .retrieve()
                .body(RoomDto.class);
    }

    public RoomDto update(String id, RoomRequestDto request) {
        return roomsClient.put()
                .uri("/api/v1/rooms/{id}", id)
                .body(request)
                .retrieve()
                .body(RoomDto.class);
    }

    public void delete(String id) {
        roomsClient.delete()
                .uri("/api/v1/rooms/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
