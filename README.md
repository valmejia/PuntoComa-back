# PuntoComa-back

API REST de **Punto&Coma** (Spring Boot 4 · Java 17 · MariaDB · JWT).

## Ejecutar

1. Crear la base de datos en MariaDB: `CREATE DATABASE puntocoma;`
2. Ajustar credenciales en `src/main/resources/application.properties` o con variables de entorno
   (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`).
3. `./mvnw spring-boot:run` (puerto 8080).

Al arrancar se crea un **administrador** (`admin@puntocoma.com` / `Admin12345`) y, si no hay libros,
un **vendedor** de ejemplo (`vendedor@puntocoma.com` / `Vendedor123`) con 6 libros.

## Pruebas

`./mvnw test` — pruebas de integración con H2 en memoria (no necesita MariaDB).
Ejemplos de peticiones en `peticiones.http`.

## Endpoints

Todas las rutas (excepto registro, login y salud) requieren `Authorization: Bearer <token>`.
Los errores siempre responden `{ status, error, mensaje, errores }`.

| Método | Ruta | Descripción | Sprint |
|---|---|---|---|
| POST | `/api/auth/registro` | Registro (rol USUARIO) | 1 |
| POST | `/api/auth/login` | Login → `{ token, usuario }` | 1 |
| GET | `/api/auth/me` | Perfil actual | 1 |
| GET | `/api/libros` | Catálogo: `q` (título, autor o ISBN), `autor, genero, formato, tipo, disponible` | 1 |
| GET | `/api/libros/{id}` · `/generos` · `/autores` | Detalle y valores para filtros | 1 |
| GET | `/api/publico/portada` | Contadores y destacados para la bienvenida (público) | 1 |
| GET/PUT | `/api/admin/usuarios`, `/{id}/rol`, `/{id}/estado` | Gestión de roles y bloqueo (ADMIN) | 1 |
| POST | `/api/libros` | Publicar libro (venta / préstamo / ambos) | 2 |
| GET | `/api/libros/mios` | Mis publicaciones | 2 |
| PUT/DELETE | `/api/libros/{id}` | Editar / eliminar (dueño, vendedor, admin) | 2 |
| POST | `/api/prestamos` | Solicitar préstamo `{ libroId, dias? }` | 2 |
| GET | `/api/prestamos/mios` · `/recibidos` · `/{id}` | Mis préstamos / de mis libros / detalle | 2 |
| GET | `/api/prestamos?estado=` | Todos los préstamos (VENDEDOR, ADMIN) | 2 |
| PUT | `/api/prestamos/{id}/devolucion` | Registrar devolución y multa | 2 |

### Reglas de préstamo
- Sólo libros con tipo `PRESTAMO` o `AMBOS`, con stock > 0, que no sean tuyos.
- Plazo 1–30 días (14 por defecto); máximo 3 préstamos activos por usuario.
- El préstamo descuenta stock al instante; con stock 0 el libro pasa a `PRESTADO`.
- La devolución repone stock y cobra **$10 por día de retraso** (configurable en `app.prestamos.*`).
