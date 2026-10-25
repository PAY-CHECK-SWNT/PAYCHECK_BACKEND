package com.paycheck.plan;

/** Alertas del plan, en el orden en que se muestran. */
public enum CodigoAlerta {
    CICLO_VENCIDO("Ya pasó la fecha de tu próximo ingreso. Actualiza tu ciclo."),
    LIBRE_NEGATIVO("Tus gastos fijos superan lo que tienes."),
    AGOTAMIENTO_ANTICIPADO("A este ritmo el dinero se acaba antes de tu próximo ingreso."),
    DATOS_INSUFICIENTES("Aún no hay datos suficientes para calcular tu ritmo de gasto.");

    private final String mensaje;

    CodigoAlerta(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
