package com.paycheck.movimiento;

/** Categorías de docs/api.md; cada una pertenece a un tipo de movimiento. */
public enum Categoria {
    COMIDA(TipoMovimiento.GASTO),
    TRANSPORTE(TipoMovimiento.GASTO),
    OCIO(TipoMovimiento.GASTO),
    COMPRAS(TipoMovimiento.GASTO),
    EDUCACION(TipoMovimiento.GASTO),
    SALUD(TipoMovimiento.GASTO),
    GASTOS_FIJOS(TipoMovimiento.GASTO),
    OTROS(TipoMovimiento.GASTO),
    MESADA(TipoMovimiento.INGRESO),
    TRABAJO(TipoMovimiento.INGRESO),
    OTROS_INGRESOS(TipoMovimiento.INGRESO);

    private final TipoMovimiento tipo;

    Categoria(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }
}
