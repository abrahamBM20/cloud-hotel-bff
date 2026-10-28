package com.hotel.bff.reservations;

import com.hotel.bff.reservations.dto.ReservationRequestDto;
import com.hotel.bff.reservations.dto.ReservationResponseDto;
import com.hotel.bff.reservations.dto.ReservationStatusRequestDto;
import com.hotel.bff.reservations.dto.ReservationStatusResponseDto;
import com.hotel.bff.rooms.RoomService;
import com.hotel.bff.rooms.dto.RoomDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final ReservationEventPublisher reservationEventPublisher;
    private final RoomService roomService;

    public ReservationService(ReservationEventPublisher reservationEventPublisher,
                              RoomService roomService) {
        this.reservationEventPublisher = reservationEventPublisher;
        this.roomService = roomService;
    }

    /**
     * Crea una reserva. No escribe nada en la base: publica el evento en SNS y la Lambda
     * 'reservation-recorder' (SQS) inserta la fila. Por eso el id se genera acá y se
     * devuelve de inmediato, aunque la fila aún no exista.
     */
    public ReservationResponseDto request(Jwt jwt, ReservationRequestDto request) {
        if (request.checkIn() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de check-in es obligatoria");
        }
        if (request.checkOut() != null && !request.checkOut().isAfter(request.checkIn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de check-out debe ser posterior a la de check-in");
        }

        String roomNumber = request.roomNumber().trim();
        RoomDto room = roomService.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La habitación " + roomNumber + " no existe"));
        if (!Boolean.TRUE.equals(room.isAvailable())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La habitación " + roomNumber + " no está disponible");
        }

        String reservationId = UUID.randomUUID().toString();
        Instant requestedAt = Instant.now();
        String requester = resolveRequester(jwt);
        String guestEmail = (request.guestEmail() == null || request.guestEmail().isBlank())
                ? requester
                : request.guestEmail().trim();
        String guestName = request.guestName().trim();
        Integer guests = request.guests() == null || request.guests() <= 0 ? 1 : request.guests();

        int nights = request.checkOut() == null
                ? 1
                : (int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        BigDecimal totalAmount = room.pricePerNight() == null
                ? BigDecimal.ZERO
                : room.pricePerNight().multiply(BigDecimal.valueOf(nights));

        reservationEventPublisher.publishRequested(reservationId, guestName, guestEmail, jwt.getSubject(),
                room.roomNumber(), room.roomType(), request.checkIn(), request.checkOut(), guests,
                totalAmount, request.notes(), requestedAt, requester);

        return new ReservationResponseDto(reservationId, guestName, guestEmail, room.roomNumber(),
                room.roomType(), request.checkIn(), request.checkOut(), guests, totalAmount,
                ReservationStatus.PENDIENTE.name(), request.notes(), requestedAt, null);
    }

    /** Cambia el estado de una reserva (check-in, check-out, cancelación). También es asíncrono. */
    public ReservationStatusResponseDto changeStatus(Jwt jwt, String reservationId,
                                                     ReservationStatusRequestDto request) {
        String newStatus = ReservationStatus.normalize(request.status());
        if (!ReservationStatus.isValid(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado de reserva inválido: " + request.status()
                            + ". Valores permitidos: " + List.of(ReservationStatus.values()));
        }

        Instant changedAt = Instant.now();
        String changedBy = resolveRequester(jwt);

        reservationEventPublisher.publishStatusChanged(reservationId, newStatus, request.notes(),
                changedBy, changedAt);

        return new ReservationStatusResponseDto(reservationId, newStatus,
                "Cambio de estado encolado. La reserva se actualiza en unos segundos.", changedAt);
    }

    /**
     * Lista las reservas. Los filtros opcionales se pasan a ms-hotel-rooms.
     * Un huésped que no es staff solo ve las suyas.
     */
    public List<ReservationResponseDto> list(Jwt jwt, String status, String roomNumber,
                                             String guestId, String id, Integer limit) {
        boolean isStaff = hasRole(jwt, "ADMIN") || hasRole(jwt, "RECEPCIONISTA");

        Map<String, String> filters = new LinkedHashMap<>();
        if (status != null && !status.isBlank()) {
            filters.put("status", ReservationStatus.normalize(status));
        }
        if (roomNumber != null && !roomNumber.isBlank()) {
            filters.put("roomNumber", roomNumber.trim());
        }
        if (id != null && !id.isBlank()) {
            filters.put("id", id.trim());
        }
        if (guestId != null && !guestId.isBlank()) {
            filters.put("guestId", guestId.trim());
        }
        if (!isStaff) {
            // Un huésped solo puede ver sus propias reservas, sin importar el filtro que pida.
            filters.put("guestId", jwt.getSubject());
        }
        filters.put("limit", String.valueOf(resolveLimit(limit)));

        return roomService.getReservations(
                filters.get("status"),
                filters.get("roomNumber"),
                filters.get("guestId"),
                filters.get("id"),
                resolveLimit(limit));
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private boolean hasRole(Jwt jwt, String role) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        return groups != null && groups.stream().anyMatch(group -> group.equalsIgnoreCase(role));
    }

    /** email -> cognito:username -> sub, el primero que venga en el JWT. */
    private String resolveRequester(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String username = jwt.getClaimAsString("cognito:username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return jwt.getSubject();
    }
}
