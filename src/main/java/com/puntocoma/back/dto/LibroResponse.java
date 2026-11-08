package com.puntocoma.back.dto;

import com.puntocoma.back.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LibroResponse(
        Long id, String titulo, String autor, String genero, String descripcion,
        String isbn, String editorial, Integer anioPublicacion,
        FormatoLibro formato, TipoPublicacion tipo, BigDecimal precio,
        int stock, EstadoLibro estado, boolean disponible, String portadaUrl,
        Publicador publicadoPor, LocalDateTime fechaPublicacion
) {
    public record Publicador(Long id, String nombre) {}

    public static LibroResponse de(Libro l) {
        Usuario p = l.getPublicadoPor();
        return new LibroResponse(l.getId(), l.getTitulo(), l.getAutor(), l.getGenero(), l.getDescripcion(),
                l.getIsbn(), l.getEditorial(), l.getAnioPublicacion(), l.getFormato(), l.getTipo(), l.getPrecio(),
                l.getStock(), l.getEstado(), l.getStock() > 0, l.getPortadaUrl(),
                new Publicador(p.getId(), p.getNombre()), l.getFechaPublicacion());
    }
}
