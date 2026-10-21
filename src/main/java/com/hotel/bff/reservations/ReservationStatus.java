package com.hotel.bff.reservations;

/**
 * Estados posibles de una reserva. Los mismos valores que usa el frontend.
 */
public enum ReservationStatus {

    PENDIENTE,
    CONFIRMADA,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELADA;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (ReservationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
