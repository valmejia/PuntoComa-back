package com.puntocoma.back.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "libros")
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 150)
    private String autor;

    @Column(nullable = false, length = 60)
    private String genero;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 20)
    private String isbn;

    @Column(length = 120)
    private String editorial;

    @Column(name = "anio_publicacion")
    private Integer anioPublicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormatoLibro formato;

    /** Venta, préstamo o ambos. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPublicacion tipo;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    /** Ejemplares disponibles en este momento. */
    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLibro estado = EstadoLibro.DISPONIBLE;

    @Column(name = "portada_url", length = 500)
    private String portadaUrl;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "publicado_por_id", nullable = false)
    private Usuario publicadoPor;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    /** Borrado lógico: un libro inactivo no aparece en el catálogo. */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean activo = true;

    @PrePersist
    void prePersist() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
    }

    public boolean admitePrestamo() {
        return tipo == TipoPublicacion.PRESTAMO || tipo == TipoPublicacion.AMBOS;
    }

    public boolean admiteVenta() {
        return tipo == TipoPublicacion.VENTA || tipo == TipoPublicacion.AMBOS;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getEditorial() { return editorial; }
    public void setEditorial(String editorial) { this.editorial = editorial; }
    public Integer getAnioPublicacion() { return anioPublicacion; }
    public void setAnioPublicacion(Integer anioPublicacion) { this.anioPublicacion = anioPublicacion; }
    public FormatoLibro getFormato() { return formato; }
    public void setFormato(FormatoLibro formato) { this.formato = formato; }
    public TipoPublicacion getTipo() { return tipo; }
    public void setTipo(TipoPublicacion tipo) { this.tipo = tipo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public EstadoLibro getEstado() { return estado; }
    public void setEstado(EstadoLibro estado) { this.estado = estado; }
    public String getPortadaUrl() { return portadaUrl; }
    public void setPortadaUrl(String portadaUrl) { this.portadaUrl = portadaUrl; }
    public Usuario getPublicadoPor() { return publicadoPor; }
    public void setPublicadoPor(Usuario publicadoPor) { this.publicadoPor = publicadoPor; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
