package com.puntocoma.back.dto;

import com.puntocoma.back.entity.FormatoLibro;
import com.puntocoma.back.entity.TipoPublicacion;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LibroRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede exceder 200 caracteres")
        String titulo,

        @NotBlank(message = "El autor es obligatorio")
        @Size(max = 150, message = "El autor no puede exceder 150 caracteres")
        String autor,

        @NotBlank(message = "El género es obligatorio")
        @Size(max = 60, message = "El género no puede exceder 60 caracteres")
        String genero,

        @Size(max = 2000, message = "La descripción no puede exceder 2000 caracteres")
        String descripcion,

        @Size(max = 20, message = "El ISBN no puede exceder 20 caracteres")
        String isbn,

        @Size(max = 120, message = "La editorial no puede exceder 120 caracteres")
        String editorial,

        @Min(value = 1000, message = "Año de publicación inválido")
        @Max(value = 2100, message = "Año de publicación inválido")
        Integer anioPublicacion,

        @NotNull(message = "El formato es obligatorio (FISICO o DIGITAL)")
        FormatoLibro formato,

        @NotNull(message = "El tipo es obligatorio (VENTA, PRESTAMO o AMBOS)")
        TipoPublicacion tipo,

        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Precio inválido")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 9999, message = "El stock no puede exceder 9999")
        Integer stock,

        @Size(max = 500, message = "La URL de portada no puede exceder 500 caracteres")
        String portadaUrl
) {}
