package com.puntocoma.back.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato JSON único para todos los errores de la API. */
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String mensaje, Map<String, String> errores) {
    public static ErrorResponse de(int status, String error, String mensaje) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, null);
    }
}
