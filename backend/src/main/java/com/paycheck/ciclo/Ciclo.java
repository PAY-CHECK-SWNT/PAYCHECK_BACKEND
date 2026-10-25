package com.paycheck.ciclo;

import com.paycheck.auth.Usuario;
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

@Entity
@Table(name = "ciclo")
public class Ciclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "saldo_inicial", nullable = false)
    private long saldoInicial;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_proximo_ingreso", nullable = false)
    private LocalDate fechaProximoIngreso;

    @Column(name = "fecha_estimada", nullable = false)
    private boolean fechaEstimada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCiclo estado;

    protected Ciclo() {
    }

    /** Crea un ciclo ACTIVO. */
    public Ciclo(Usuario usuario, long saldoInicial, LocalDate fechaInicio,
                 LocalDate fechaProximoIngreso, boolean fechaEstimada) {
        this.usuario = usuario;
        this.saldoInicial = saldoInicial;
        this.fechaInicio = fechaInicio;
        this.fechaProximoIngreso = fechaProximoIngreso;
        this.fechaEstimada = fechaEstimada;
        this.estado = EstadoCiclo.ACTIVO;
    }

    public void cambiarFechaProximoIngreso(LocalDate fecha, boolean estimada) {
        this.fechaProximoIngreso = fecha;
        this.fechaEstimada = estimada;
    }

    public void cerrar() {
        this.estado = EstadoCiclo.CERRADO;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public long getSaldoInicial() {
        return saldoInicial;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaProximoIngreso() {
        return fechaProximoIngreso;
    }

    public boolean isFechaEstimada() {
        return fechaEstimada;
    }

    public EstadoCiclo getEstado() {
        return estado;
    }
}
