package com.hotel.bff.reservations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publica los eventos de reservas en el topic SNS del hotel.
 *
 * El topic tiene dos suscripciones SQS, cada una con su FilterPolicy sobre el
 * campo "type" del cuerpo del mensaje:
 *   - type = reservation.requested      -> cola de la Lambda 'reservation-recorder'
 *   - type = reservation.status-changed  -> cola de la Lambda 'reservation-status-updater'
 *
 * El formato del mensaje es {type, body}, igual que en el BFF de chati.
 */
@Component
public class ReservationEventPublisher {

    /** Valor de "type" para una nueva reserva. */
    static final String TYPE_REQUESTED = "reservation.requested";

    /** Valor de "type" para un cambio de estado (check-in, check-out, cancelación). */
    static final String TYPE_STATUS_CHANGED = "reservation.status-changed";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SnsClient snsClient;
    private final String topicArn;

    public ReservationEventPublisher(SnsClient snsClient, @Value("${aws.sns.reservation-topic-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    /** Publica la solicitud de reserva. Retorna el messageId de SNS. */
    public String publishRequested(String reservationId, String guestName, String guestEmail, String guestId,
                                   String roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                                   Integer guests, BigDecimal totalAmount, String notes, Instant requestedAt,
                                   String requestedBy) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reservationId", reservationId);
        body.put("guestName", guestName);
        body.put("guestEmail", guestEmail);
        body.put("guestId", guestId);
        body.put("roomNumber", roomNumber);
        body.put("roomType", roomType);
        body.put("checkIn", checkIn == null ? null : checkIn.toString());
        body.put("checkOut", checkOut == null ? null : checkOut.toString());
        body.put("guests", guests);
        body.put("totalAmount", totalAmount);
        body.put("status", ReservationStatus.PENDIENTE.name());
        body.put("notes", notes);
        body.put("requestedAt", requestedAt.toString());
        body.put("requestedBy", requestedBy);

        return publish(TYPE_REQUESTED, body);
    }

    /** Publica el cambio de estado de una reserva. Retorna el messageId de SNS. */
    public String publishStatusChanged(String reservationId, String newStatus, String notes,
                                       String changedBy, Instant changedAt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reservationId", reservationId);
        body.put("newStatus", newStatus);
        body.put("notes", notes);
        body.put("changedBy", changedBy);
        body.put("changedAt", changedAt.toString());

        return publish(TYPE_STATUS_CHANGED, body);
    }

    private String publish(String type, Map<String, Object> body) {
        try {
            String message = OBJECT_MAPPER.writeValueAsString(Map.of("type", type, "body", body));

            return snsClient.publish(PublishRequest.builder()
                            .topicArn(topicArn)
                            .message(message)
                            .build())
                    .messageId();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo publicar en SNS", e);
        }
    }
}
