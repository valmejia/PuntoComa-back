package com.puntocoma.back.service;

import com.puntocoma.back.dto.LibroRequest;
import com.puntocoma.back.dto.LibroResponse;
import com.puntocoma.back.entity.*;
import com.puntocoma.back.exception.ApiException;
import com.puntocoma.back.repository.LibroRepository;
import com.puntocoma.back.repository.PrestamoRepository;
import com.puntocoma.back.repository.UsuarioRepository;
import com.puntocoma.back.security.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class LibroService {

    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;

    public LibroService(LibroRepository libroRepository, UsuarioRepository usuarioRepository,
                        PrestamoRepository prestamoRepository) {
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
    }

    /** Filtros del catálogo (HU-03). Todos son opcionales. */
    public record FiltroCatalogo(String q, String autor, String genero, FormatoLibro formato,
                                 TipoPublicacion tipo, Boolean disponible) {}

    /** HU-03 / CU-03: búsqueda por título y filtros por autor, género, formato, tipo y disponibilidad. */
    @Transactional(readOnly = true)
    public List<LibroResponse> buscar(FiltroCatalogo f) {
        Specification<Libro> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("activo")));
            if (StringUtils.hasText(f.q())) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("titulo")), like), cb.like(cb.lower(root.get("autor")), like)));
            }
            if (StringUtils.hasText(f.autor())) {
                p.add(cb.like(cb.lower(root.get("autor")), "%" + f.autor().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(f.genero())) {
                p.add(cb.equal(cb.lower(root.get("genero")), f.genero().trim().toLowerCase(Locale.ROOT)));
            }
            if (f.formato() != null) {
                p.add(cb.equal(root.get("formato"), f.formato()));
            }
            if (f.tipo() != null) {
                // Un libro "AMBOS" aparece tanto al filtrar por VENTA como por PRESTAMO.
                p.add(f.tipo() == TipoPublicacion.AMBOS
                        ? cb.equal(root.get("tipo"), TipoPublicacion.AMBOS)
                        : root.get("tipo").in(f.tipo(), TipoPublicacion.AMBOS));
            }
            if (Boolean.TRUE.equals(f.disponible())) {
                p.add(cb.greaterThan(root.get("stock"), 0));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return libroRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaPublicacion").and(Sort.by("id").descending()))
                .stream().map(LibroResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return LibroResponse.de(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<String> generos() {
        return libroRepository.findGenerosActivos();
    }

    @Transactional(readOnly = true)
    public List<String> autores() {
        return libroRepository.findAutoresActivos();
    }

    @Transactional(readOnly = true)
    public List<LibroResponse> misLibros(Long usuarioId) {
        return libroRepository.findByPublicadoPorIdAndActivoTrueOrderByFechaPublicacionDesc(usuarioId)
                .stream().map(LibroResponse::de).toList();
    }

    /** HU-04 / CU-04: publicar libro para venta y/o préstamo. Aparece de inmediato en el catálogo. */
    @Transactional
    public LibroResponse publicar(LibroRequest req, UsuarioAutenticado actual) {
        Usuario publicador = usuarioRepository.findById(actual.id())
                .orElseThrow(() -> ApiException.noAutorizado("Usuario no encontrado"));
        Libro l = new Libro();
        aplicar(l, req);
        l.setPublicadoPor(publicador);
        l.setEstado(l.getStock() > 0 ? EstadoLibro.DISPONIBLE : EstadoLibro.AGOTADO);
        return LibroResponse.de(libroRepository.save(l));
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest req, UsuarioAutenticado actual) {
        Libro l = libroRepository.findActivoParaActualizar(id)
                .orElseThrow(() -> ApiException.noEncontrado("Libro no encontrado"));
        verificarPermiso(l, actual);
        aplicar(l, req);
        recalcularEstado(l);
        return LibroResponse.de(l);
    }

    /** Borrado lógico; no se permite si hay préstamos activos del libro. */
    @Transactional
    public void eliminar(Long id, UsuarioAutenticado actual) {
        Libro l = buscarActivo(id);
        verificarPermiso(l, actual);
        if (prestamoRepository.existsByLibroIdAndEstado(id, EstadoPrestamo.ACTIVO)) {
            throw ApiException.conflicto("No se puede eliminar: el libro tiene préstamos activos");
        }
        l.setActivo(false);
    }

    /** DISPONIBLE si hay ejemplares; si no, PRESTADO (hay préstamos activos) o AGOTADO. */
    void recalcularEstado(Libro l) {
        if (l.getStock() > 0) {
            l.setEstado(EstadoLibro.DISPONIBLE);
        } else if (l.getId() != null && prestamoRepository.existsByLibroIdAndEstado(l.getId(), EstadoPrestamo.ACTIVO)) {
            l.setEstado(EstadoLibro.PRESTADO);
        } else {
            l.setEstado(EstadoLibro.AGOTADO);
        }
    }

    private Libro buscarActivo(Long id) {
        return libroRepository.findByIdAndActivoTrue(id).orElseThrow(() -> ApiException.noEncontrado("Libro no encontrado"));
    }

    private static void verificarPermiso(Libro l, UsuarioAutenticado actual) {
        if (!actual.esStaff() && !Objects.equals(l.getPublicadoPor().getId(), actual.id())) {
            throw ApiException.prohibido("Sólo quien publicó el libro (o un vendedor/administrador) puede modificarlo");
        }
    }

    private static void aplicar(Libro l, LibroRequest r) {
        if (r.tipo() != TipoPublicacion.PRESTAMO
                && (r.precio() == null || r.precio().compareTo(BigDecimal.ZERO) <= 0)) {
            throw ApiException.badRequest("El precio es obligatorio y debe ser mayor a 0 para libros en venta");
        }
        l.setTitulo(r.titulo().trim());
        l.setAutor(r.autor().trim());
        l.setGenero(capitalizar(r.genero().trim()));
        l.setDescripcion(vacioANull(r.descripcion()));
        l.setIsbn(vacioANull(r.isbn()));
        l.setEditorial(vacioANull(r.editorial()));
        l.setAnioPublicacion(r.anioPublicacion());
        l.setFormato(r.formato());
        l.setTipo(r.tipo());
        l.setPrecio(r.tipo() == TipoPublicacion.PRESTAMO ? null : r.precio());
        l.setStock(r.stock());
        l.setPortadaUrl(vacioANull(r.portadaUrl()));
    }

    private static String vacioANull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    /** Normaliza el género ("ciencia ficción" -> "Ciencia ficción") para que el filtro no duplique valores. */
    private static String capitalizar(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
