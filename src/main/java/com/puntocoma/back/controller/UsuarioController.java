package com.puntocoma.back.controller;

import com.puntocoma.back.dto.RegistroRequest;
import com.puntocoma.back.dto.UsuarioResponse;
import com.puntocoma.back.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // Permite que React Native se conecte
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody RegistroRequest request) {
        try {
            UsuarioResponse response = usuarioService.registrarUsuario(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}