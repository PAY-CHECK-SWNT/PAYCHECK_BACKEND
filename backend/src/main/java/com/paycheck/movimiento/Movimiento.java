package com.paycheck.movimiento;

import com.paycheck.auth.Usuario;
import com.paycheck.ciclo.Ciclo;
import com.paycheck.compromiso.Compromiso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "movimiento")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private long monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private LocalDate fecha;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenMovimiento origen;

    /** Compromiso que este gasto paga, si aplica. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compromiso_id")
    private Compromiso compromiso;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Movimiento() {
    }

    public Movimiento(Usuario usuario, Ciclo ciclo, TipoMovimiento tipo, long monto,
                      Categoria categoria, LocalDate fecha, String descripcion,
                      OrigenMovimiento origen) {
        this.usuario = usuario;
        this.ciclo = ciclo;
        this.tipo = tipo;
        this.monto = monto;
        this.categoria = categoria;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.origen = origen;
    }

    public void asociarCompromiso(Compromiso compromiso) {
        this.compromiso = compromiso;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public long getMonto() {
        return monto;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public OrigenMovimiento getOrigen() {
        return origen;
    }

    public Compromiso getCompromiso() {
        return compromiso;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
