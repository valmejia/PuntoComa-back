package com.puntocoma.back.repository;

import com.puntocoma.back.entity.EstadoPrestamo;
import com.puntocoma.back.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDescIdDesc(Long usuarioId);

    List<Prestamo> findByLibroPublicadoPorIdOrderByFechaPrestamoDescIdDesc(Long usuarioId);

    List<Prestamo> findAllByOrderByFechaPrestamoDescIdDesc();

    List<Prestamo> findByEstadoOrderByFechaPrestamoDescIdDesc(EstadoPrestamo estado);

    long countByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    boolean existsByUsuarioIdAndLibroIdAndEstado(Long usuarioId, Long libroId, EstadoPrestamo estado);

    boolean existsByLibroIdAndEstado(Long libroId, EstadoPrestamo estado);
}
