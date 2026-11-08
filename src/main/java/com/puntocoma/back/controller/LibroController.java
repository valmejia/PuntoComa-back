package com.puntocoma.back.controller;

import com.puntocoma.back.dto.LibroRequest;
import com.puntocoma.back.dto.LibroResponse;
import com.puntocoma.back.entity.FormatoLibro;
import com.puntocoma.back.entity.TipoPublicacion;
import com.puntocoma.back.security.UsuarioAutenticado;
import com.puntocoma.back.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    /** Catálogo: GET /api/libros?q=&autor=&genero=&formato=FISICO|DIGITAL&tipo=VENTA|PRESTAMO|AMBOS&disponible=true */
    @GetMapping
    public List<LibroResponse> catalogo(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String autor,
                                        @RequestParam(required = false) String genero,
                                        @RequestParam(required = false) FormatoLibro formato,
                                        @RequestParam(required = false) TipoPublicacion tipo,
                                        @RequestParam(required = false) Boolean disponible) {
        return libroService.buscar(new LibroService.FiltroCatalogo(q, autor, genero, formato, tipo, disponible));
    }

    @GetMapping("/generos")
    public List<String> generos() {
        return libroService.generos();
    }

    @GetMapping("/autores")
    public List<String> autores() {
        return libroService.autores();
    }

    @GetMapping("/mios")
    public List<LibroResponse> mios(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return libroService.misLibros(actual.id());
    }

    @GetMapping("/{id}")
    public LibroResponse obtener(@PathVariable Long id) {
        return libroService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibroResponse publicar(@Valid @RequestBody LibroRequest req, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return libroService.publicar(req, actual);
    }

    @PutMapping("/{id}")
    public LibroResponse actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest req,
                                    @AuthenticationPrincipal UsuarioAutenticado actual) {
        return libroService.actualizar(id, req, actual);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        libroService.eliminar(id, actual);
    }
}
