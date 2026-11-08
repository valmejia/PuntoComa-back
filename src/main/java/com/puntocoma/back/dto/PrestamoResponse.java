package com.puntocoma.back.dto;

import com.puntocoma.back.entity.FormatoLibro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code estado}: ACTIVO, VENCIDO (activo y fuera de plazo) o DEVUELTO.
 * {@code multa}: multa final si ya se devolvió, o multa acumulada a la fecha si sigue activo.
 */
public record PrestamoResponse(
        Long id, LibroResumen libro, Prestatario usuario,
        LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada, LocalDate fechaDevolucionReal,
        String estado, long diasRetraso, BigDecimal multa
) {
    public record LibroResumen(Long id, String titulo, String autor, FormatoLibro formato, String portadaUrl, Long publicadoPorId) {}
    public record Prestatario(Long id, String nombre, String email) {}
}
