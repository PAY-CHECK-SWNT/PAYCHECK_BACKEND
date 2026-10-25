package com.paycheck.compromiso;

import com.paycheck.ciclo.Ciclo;
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

/** Gasto fijo que se espera pagar dentro del ciclo. */
@Entity
@Table(name = "compromiso")
public class Compromiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private long monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompromiso estado;

    protected Compromiso() {
    }

    public Compromiso(Ciclo ciclo, String nombre, long monto) {
        this.ciclo = ciclo;
        this.nombre = nombre;
        this.monto = monto;
        this.estado = EstadoCompromiso.PENDIENTE;
    }

    public void marcarPagado() {
        this.estado = EstadoCompromiso.PAGADO;
    }

    public Long getId() {
        return id;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public String getNombre() {
        return nombre;
    }

    public long getMonto() {
        return monto;
    }

    public EstadoCompromiso getEstado() {
        return estado;
    }
}
