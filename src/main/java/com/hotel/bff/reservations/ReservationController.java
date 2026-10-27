package com.hotel.bff.reservations;

import com.hotel.bff.reservations.dto.ReservationRequestDto;
import com.hotel.bff.reservations.dto.ReservationResponseDto;
import com.hotel.bff.reservations.dto.ReservationStatusRequestDto;
import com.hotel.bff.reservations.dto.ReservationStatusResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * 202: aceptado. SNS hotel-reservation-events -> SQS -> Lambda reservation-recorder
     * guarda la reserva. La lectura posterior la resuelve la Lambda reservation-query.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponseDto> create(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody ReservationRequestDto request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(reservationService.request(jwt, request));
    }

    /** Cambio de estado (check-in / check-out / cancelación). También asíncrono. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ReservationStatusResponseDto> changeStatus(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable String id,
                                                                     @Valid @RequestBody ReservationStatusRequestDto request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(reservationService.changeStatus(jwt, id, request));
    }

    /** Listado. La Lambda 'reservation-query' lee la tabla y devuelve el JSON. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'HUESPED')")
    public List<ReservationResponseDto> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String roomNumber,
                                             @RequestParam(required = false) String guestId,
                                             @RequestParam(required = false) String id,
                                             @RequestParam(required = false) Integer limit) {
        return reservationService.list(jwt, status, roomNumber, guestId, id, limit);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'HUESPED')")
    public ResponseEntity<ReservationResponseDto> getById(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable String id) {
        List<ReservationResponseDto> found = reservationService.list(jwt, null, null, null, id, 1);
        return found.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(found.get(0));
    }
}
