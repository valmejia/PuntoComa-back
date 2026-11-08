package com.puntocoma.back.service;

import com.puntocoma.back.dto.PrestamoRequest;
import com.puntocoma.back.dto.PrestamoResponse;
import com.puntocoma.back.entity.*;
import com.puntocoma.back.exception.ApiException;
import com.puntocoma.back.repository.LibroRepository;
import com.puntocoma.back.repository.PrestamoRepository;
import com.puntocoma.back.repository.UsuarioRepository;
import com.puntocoma.back.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroService libroService;
    private final Clock clock;
    private final int diasPorDefecto;
    private final int maxActivos;
    private final BigDecimal multaPorDia;

    public PrestamoService(PrestamoRepository prestamoRepository, LibroRepository libroRepository,
                           UsuarioRepository usuarioRepository, LibroService libroService, Clock clock,
                           @Value("${app.prestamos.dias-default}") int diasPorDefecto,
                           @Value("${app.prestamos.max-activos}") int maxActivos,
                           @Value("${app.prestamos.multa-por-dia}") BigDecimal multaPorDia) {
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroService = libroService;
        this.clock = clock;
        this.diasPorDefecto = diasPorDefecto;
        this.maxActivos = maxActivos;
        this.multaPorDia = multaPorDia;
    }

    /** HU-05 / CU-05: el préstamo descuenta stock en el momento (inventario en tiempo real, CA-02). */
    @Transactional
    public PrestamoResponse solicitar(PrestamoRequest req, UsuarioAutenticado actual) {
        Libro libro = libroRepository.findActivoParaActualizar(req.libroId())
                .orElseThrow(() -> ApiException.noEncontrado("Libro no encontrado"));
        if (!libro.admitePrestamo()) {
            throw ApiException.badRequest("Este libro sólo está disponible para venta");
        }
        if (Objects.equals(libro.getPublicadoPor().getId(), actual.id())) {
            throw ApiException.badRequest("No puedes pedir prestado un libro que tú publicaste");
        }
        if (libro.getStock() <= 0) {
            throw ApiException.conflicto("El libro no está disponible en este momento");
        }
        if (prestamoRepository.existsByUsuarioIdAndLibroIdAndEstado(actual.id(), libro.getId(), EstadoPrestamo.ACTIVO)) {
            throw ApiException.conflicto("Ya tienes un préstamo activo de este libro");
        }
        if (prestamoRepository.countByUsuarioIdAndEstado(actual.id(), EstadoPrestamo.ACTIVO) >= maxActivos) {
            throw ApiException.conflicto("Alcanzaste el máximo de " + maxActivos + " préstamos activos");
        }
        Usuario usuario = usuarioRepository.findById(actual.id())
                .orElseThrow(() -> ApiException.noAutorizado("Usuario no encontrado"));

        LocalDate hoy = LocalDate.now(clock);
        int dias = req.dias() != null ? req.dias() : diasPorDefecto;
        Prestamo p = new Prestamo();
        p.setLibro(libro);
        p.setUsuario(usuario);
        p.setFechaPrestamo(hoy);
        p.setFechaDevolucionEsperada(hoy.plusDays(dias));
        p.setEstado(EstadoPrestamo.ACTIVO);
        p.setMulta(BigDecimal.ZERO);
        prestamoRepository.save(p);

        libro.setStock(libro.getStock() - 1);
        if (libro.getStock() == 0) {
            libro.setEstado(EstadoLibro.PRESTADO);
        }
        return aRespuesta(p);
    }

    /** HU-14 / CU-14: registra la devolución, repone stock y calcula la multa por retraso. */
    @Transactional
    public PrestamoResponse devolver(Long prestamoId, UsuarioAutenticado actual) {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> ApiException.noEncontrado("Préstamo no encontrado"));
        boolean esPrestatario = Objects.equals(p.getUsuario().getId(), actual.id());
        boolean esDueno = Objects.equals(p.getLibro().getPublicadoPor().getId(), actual.id());
        if (!esPrestatario && !esDueno && !actual.esStaff()) {
            throw ApiException.prohibido("No puedes registrar la devolución de este préstamo");
        }
        if (p.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw ApiException.conflicto("Este préstamo ya fue devuelto");
        }
        LocalDate hoy = LocalDate.now(clock);
        p.setFechaDevolucionReal(hoy);
        p.setEstado(EstadoPrestamo.DEVUELTO);
        p.setMulta(calcularMulta(p.getFechaDevolucionEsperada(), hoy));

        Libro libro = libroRepository.findById(p.getLibro().getId()).orElseThrow();
        libro.setStock(libro.getStock() + 1);
        libroService.recalcularEstado(libro);
        return aRespuesta(p);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> misPrestamos(Long usuarioId) {
        return prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDescIdDesc(usuarioId).stream().map(this::aRespuesta).toList();
    }

    /** Préstamos de los libros que publicó el usuario (para que el dueño registre devoluciones). */
    @Transactional(readOnly = true)
    public List<PrestamoResponse> prestamosDeMisLibros(Long usuarioId) {
        return prestamoRepository.findByLibroPublicadoPorIdOrderByFechaPrestamoDescIdDesc(usuarioId).stream().map(this::aRespuesta).toList();
    }

    /** Todos los préstamos (VENDEDOR / ADMIN), opcionalmente filtrados por estado. */
    @Transactional(readOnly = true)
    public List<PrestamoResponse> todos(EstadoPrestamo estado) {
        List<Prestamo> lista = estado == null
                ? prestamoRepository.findAllByOrderByFechaPrestamoDescIdDesc()
                : prestamoRepository.findByEstadoOrderByFechaPrestamoDescIdDesc(estado);
        return lista.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public PrestamoResponse obtener(Long id, UsuarioAutenticado actual) {
        Prestamo p = prestamoRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("Préstamo no encontrado"));
        boolean puedeVer = actual.esStaff() || Objects.equals(p.getUsuario().getId(), actual.id())
                || Objects.equals(p.getLibro().getPublicadoPor().getId(), actual.id());
        if (!puedeVer) {
            throw ApiException.prohibido("No puedes ver este préstamo");
        }
        return aRespuesta(p);
    }

    BigDecimal calcularMulta(LocalDate esperada, LocalDate devolucion) {
        long retraso = Math.max(0, ChronoUnit.DAYS.between(esperada, devolucion));
        return multaPorDia.multiply(BigDecimal.valueOf(retraso));
    }

    private PrestamoResponse aRespuesta(Prestamo p) {
        LocalDate hoy = LocalDate.now(clock);
        boolean devuelto = p.getEstado() == EstadoPrestamo.DEVUELTO;
        LocalDate referencia = devuelto ? p.getFechaDevolucionReal() : hoy;
        long retraso = Math.max(0, ChronoUnit.DAYS.between(p.getFechaDevolucionEsperada(), referencia));
        String estado = devuelto ? "DEVUELTO" : (retraso > 0 ? "VENCIDO" : "ACTIVO");
        BigDecimal multa = devuelto ? p.getMulta() : calcularMulta(p.getFechaDevolucionEsperada(), hoy);
        Libro l = p.getLibro();
        Usuario u = p.getUsuario();
        return new PrestamoResponse(p.getId(),
                new PrestamoResponse.LibroResumen(l.getId(), l.getTitulo(), l.getAutor(), l.getFormato(), l.getPortadaUrl(),
                        l.getPublicadoPor().getId()),
                new PrestamoResponse.Prestatario(u.getId(), u.getNombre(), u.getEmail()),
                p.getFechaPrestamo(), p.getFechaDevolucionEsperada(), p.getFechaDevolucionReal(),
                estado, retraso, multa);
    }
}
