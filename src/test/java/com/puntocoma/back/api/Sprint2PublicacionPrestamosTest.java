package com.puntocoma.back.api;

import com.puntocoma.back.repository.PrestamoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprint 2: HU-04 publicar libro, HU-05 solicitar préstamo, HU-14 devolución con multa, CA-02 inventario. */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint2PublicacionPrestamosTest extends ApiTestSupport {

    @Autowired
    PrestamoRepository prestamoRepository;

    @Test
    void publicarLibroApareceEnCatalogoYEnMisLibros() throws Exception {
        String t = nuevoUsuario();
        long id = publicar(t, libroJson("Mi libro", "Yo", "ensayo", "FISICO", "AMBOS", "150.50", 2));
        get("/api/libros/" + id, t).andExpect(status().isOk())
                .andExpect(jsonPath("$.genero").value("Ensayo"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.disponible").value(true));
        get("/api/libros/mios", t).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(id));
        get("/api/libros?q=Mi libro", nuevoUsuario()).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void publicarValidaCamposObligatoriosYPrecio() throws Exception {
        String t = nuevoUsuario();
        postJson("/api/libros", t, "{\"titulo\":\"\",\"formato\":\"FISICO\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.autor").exists())
                .andExpect(jsonPath("$.errores.tipo").exists());
        postJson("/api/libros", t, libroJson("T", "A", "G", "FISICO", "VENTA", null, 1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El precio es obligatorio y debe ser mayor a 0 para libros en venta"));
        // Un libro sólo de préstamo no necesita precio
        postJson("/api/libros", t, libroJson("T", "A", "G", "FISICO", "PRESTAMO", null, 1)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.precio").doesNotExist());
    }

    @Test
    void soloElDuenoOStaffPuedeEditarYEliminar() throws Exception {
        String dueno = nuevoUsuario();
        String otro = nuevoUsuario();
        long id = publicar(dueno, libroJson("Editable", "A", "G", "FISICO", "PRESTAMO", null, 1));
        putJson("/api/libros/" + id, otro, libroJson("Hack", "A", "G", "FISICO", "PRESTAMO", null, 1)).andExpect(status().isForbidden());
        putJson("/api/libros/" + id, dueno, libroJson("Editado", "A", "G", "FISICO", "PRESTAMO", null, 5))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Editado")).andExpect(jsonPath("$.stock").value(5));
        delete("/api/libros/" + id, otro).andExpect(status().isForbidden());
        delete("/api/libros/" + id, tokenAdmin()).andExpect(status().isNoContent());
        get("/api/libros/" + id, dueno).andExpect(status().isNotFound());
    }

    @Test
    void prestamoDescuentaStockYCambiaEstadoAPrestado() throws Exception {
        String dueno = nuevoUsuario();
        String lector = nuevoUsuario();
        long libro = publicar(dueno, libroJson("Único", "A", "G", "FISICO", "PRESTAMO", null, 1));

        postJson("/api/prestamos", lector, "{\"libroId\":" + libro + ",\"dias\":7}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaPrestamo").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.fechaDevolucionEsperada").value(LocalDate.now().plusDays(7).toString()));

        get("/api/libros/" + libro, lector).andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.estado").value("PRESTADO"))
                .andExpect(jsonPath("$.disponible").value(false));

        // Ya no hay ejemplares
        postJson("/api/prestamos", nuevoUsuario(), "{\"libroId\":" + libro + "}").andExpect(status().isConflict());
        get("/api/prestamos/mios", lector).andExpect(jsonPath("$", hasSize(1)));
        get("/api/prestamos/recibidos", dueno).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void reglasDeNegocioDelPrestamo() throws Exception {
        String dueno = nuevoUsuario();
        String lector = nuevoUsuario();
        long soloVenta = publicar(dueno, libroJson("Venta", "A", "G", "FISICO", "VENTA", "10.00", 5));
        long prestable = publicar(dueno, libroJson("Prestable", "A", "G", "FISICO", "PRESTAMO", null, 5));

        postJson("/api/prestamos", lector, "{\"libroId\":" + soloVenta + "}").andExpect(status().isBadRequest());
        postJson("/api/prestamos", dueno, "{\"libroId\":" + prestable + "}").andExpect(status().isBadRequest());
        postJson("/api/prestamos", lector, "{\"libroId\":" + prestable + ",\"dias\":60}").andExpect(status().isBadRequest());
        postJson("/api/prestamos", lector, "{\"libroId\":999999}").andExpect(status().isNotFound());

        postJson("/api/prestamos", lector, "{\"libroId\":" + prestable + "}").andExpect(status().isCreated());
        postJson("/api/prestamos", lector, "{\"libroId\":" + prestable + "}").andExpect(status().isConflict());

        // Máximo 3 préstamos activos
        for (int i = 0; i < 2; i++) {
            long otro = publicar(dueno, libroJson("Extra " + i, "A", "G", "FISICO", "PRESTAMO", null, 1));
            postJson("/api/prestamos", lector, "{\"libroId\":" + otro + "}").andExpect(status().isCreated());
        }
        long cuarto = publicar(dueno, libroJson("Cuarto", "A", "G", "FISICO", "PRESTAMO", null, 1));
        postJson("/api/prestamos", lector, "{\"libroId\":" + cuarto + "}").andExpect(status().isConflict());

        // No se puede eliminar un libro con préstamos activos
        delete("/api/libros/" + prestable, dueno).andExpect(status().isConflict());
    }

    @Test
    void devolucionATiempoReponeStockSinMulta() throws Exception {
        String dueno = nuevoUsuario();
        String lector = nuevoUsuario();
        long libro = publicar(dueno, libroJson("Devolver", "A", "G", "FISICO", "AMBOS", "50.00", 1));
        long prestamo = id(postJson("/api/prestamos", lector, "{\"libroId\":" + libro + "}"));

        putJson("/api/prestamos/" + prestamo + "/devolucion", nuevoUsuario(), null).andExpect(status().isForbidden());
        putJson("/api/prestamos/" + prestamo + "/devolucion", lector, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.fechaDevolucionReal").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.multa").value(0));
        get("/api/libros/" + libro, lector).andExpect(jsonPath("$.stock").value(1)).andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        putJson("/api/prestamos/" + prestamo + "/devolucion", lector, null).andExpect(status().isConflict());
    }

    @Test
    void devolucionFueraDePlazoCalculaMulta() throws Exception {
        String dueno = nuevoUsuario();
        String lector = nuevoUsuario();
        long libro = publicar(dueno, libroJson("Tarde", "A", "G", "FISICO", "PRESTAMO", null, 1));
        long prestamo = id(postJson("/api/prestamos", lector, "{\"libroId\":" + libro + "}"));

        // Simula que el plazo venció hace 3 días
        var p = prestamoRepository.findById(prestamo).orElseThrow();
        p.setFechaPrestamo(LocalDate.now().minusDays(17));
        p.setFechaDevolucionEsperada(LocalDate.now().minusDays(3));
        prestamoRepository.save(p);

        get("/api/prestamos/" + prestamo, lector).andExpect(jsonPath("$.estado").value("VENCIDO"))
                .andExpect(jsonPath("$.diasRetraso").value(3));
        // El dueño del libro también puede registrar la devolución
        putJson("/api/prestamos/" + prestamo + "/devolucion", dueno, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.multa").value(30.0));
    }

    @Test
    void soloVendedorOAdminVenTodosLosPrestamos() throws Exception {
        get("/api/prestamos", nuevoUsuario()).andExpect(status().isForbidden());
        get("/api/prestamos?estado=ACTIVO", tokenAdmin()).andExpect(status().isOk());
    }
}
