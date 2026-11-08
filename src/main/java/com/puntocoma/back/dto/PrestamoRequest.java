package com.puntocoma.back.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** {@code dias} es opcional; si no se envía se usa el plazo por defecto. */
public record PrestamoRequest(
        @NotNull(message = "El libro es obligatorio") Long libroId,
        @Min(value = 1, message = "El préstamo debe ser de al menos 1 día")
        @Max(value = 30, message = "El préstamo no puede exceder 30 días")
        Integer dias
) {}
