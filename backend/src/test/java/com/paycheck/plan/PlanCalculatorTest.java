package com.paycheck.plan;

import static com.paycheck.plan.CodigoAlerta.AGOTAMIENTO_ANTICIPADO;
import static com.paycheck.plan.CodigoAlerta.CICLO_VENCIDO;
import static com.paycheck.plan.CodigoAlerta.DATOS_INSUFICIENTES;
import static com.paycheck.plan.CodigoAlerta.LIBRE_NEGATIVO;
import static org.assertj.core.api.Assertions.assertThat;

import com.paycheck.compromiso.EstadoCompromiso;
import com.paycheck.movimiento.TipoMovimiento;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PlanCalculatorTest {

    private final PlanCalculator calculator = new PlanCalculator();

    /** Ciclo usado por los casos A y B del documento. */
    private static final DatosCiclo CICLO_OCTUBRE = ciclo(600_000, "2026-10-20", "2026-11-03");

    @ParameterizedTest(name = "{0}")
    @MethodSource("escenarios")
    void calculaElPlan(String nombre, DatosCiclo ciclo, List<MovimientoPlan> movimientos,
                       List<CompromisoPlan> compromisos, LocalDate hoy, ResumenPlan esperado) {
        assertThat(calculator.calcular(ciclo, movimientos, compromisos, hoy)).isEqualTo(esperado);
    }

    static Stream<Arguments> escenarios() {
        return Stream.of(
                escenario("Caso A: ciclo normal",
                        CICLO_OCTUBRE,
                        List.of(gasto(120_000, "2026-10-21"), gasto(80_000, "2026-10-23"), gasto(15_000, "2026-10-25")),
                        List.of(pendiente(50_000)),
                        "2026-10-25",
                        385_000, 50_000, 335_000, 9, 38_888, 23_888, 257_216, 6, 35_833L, 12_503L, null),

                escenario("Caso B: se acaba antes del ingreso",
                        CICLO_OCTUBRE,
                        List.of(gasto(300_000, "2026-10-21"), gasto(60_000, "2026-10-24")),
                        List.of(),
                        "2026-10-25",
                        240_000, 0, 240_000, 9, 26_666, 26_666, 186_662, 6, 60_000L, -300_000L, "2026-10-29",
                        AGOTAMIENTO_ANTICIPADO),

                escenario("Caso C: libre negativo al inicio del ciclo",
                        ciclo(200_000, "2026-10-24", "2026-11-15"),
                        List.of(gasto(150_000, "2026-10-24")),
                        List.of(pendiente(100_000)),
                        "2026-10-25",
                        50_000, 100_000, -50_000, 21, 0, 0, 0, 2, null, null, null,
                        LIBRE_NEGATIVO, DATOS_INSUFICIENTES),

                escenario("Último día del ciclo: un día restante",
                        CICLO_OCTUBRE,
                        List.of(gasto(100_000, "2026-10-21"), gasto(20_000, "2026-11-02")),
                        List.of(),
                        "2026-11-02",
                        480_000, 0, 480_000, 1, 500_000, 480_000, 480_000, 14, 8_571L, 471_429L, null),

                escenario("Libre negativo con ritmo: sin fecha de agotamiento",
                        ciclo(300_000, "2026-10-20", "2026-11-03"),
                        List.of(gasto(240_000, "2026-10-21")),
                        List.of(pendiente(100_000)),
                        "2026-10-25",
                        60_000, 100_000, -40_000, 9, 0, 0, 0, 6, 40_000L, -400_000L, null,
                        LIBRE_NEGATIVO),

                escenario("Libre en cero con ritmo: se agota hoy",
                        ciclo(300_000, "2026-10-20", "2026-11-03"),
                        List.of(gasto(240_000, "2026-10-21")),
                        List.of(pendiente(60_000)),
                        "2026-10-25",
                        60_000, 60_000, 0, 9, 0, 0, 0, 6, 40_000L, -360_000L, "2026-10-25",
                        AGOTAMIENTO_ANTICIPADO),

                escenario("Sin gastos: ritmo cero",
                        ciclo(450_000, "2026-10-20", "2026-11-03"),
                        List.of(),
                        List.of(),
                        "2026-10-25",
                        450_000, 0, 450_000, 9, 50_000, 50_000, 350_000, 6, 0L, 450_000L, null),

                escenario("Menos de 3 días transcurridos: primer día del ciclo",
                        ciclo(450_000, "2026-10-25", "2026-11-03"),
                        List.of(gasto(30_000, "2026-10-25")),
                        List.of(),
                        "2026-10-25",
                        420_000, 0, 420_000, 9, 50_000, 20_000, 320_000, 1, null, null, null,
                        DATOS_INSUFICIENTES),

                escenario("Exactamente 3 días transcurridos: ya hay ritmo",
                        ciclo(450_000, "2026-10-23", "2026-11-03"),
                        List.of(gasto(30_000, "2026-10-23"), gasto(30_000, "2026-10-24")),
                        List.of(),
                        "2026-10-25",
                        390_000, 0, 390_000, 9, 43_333, 43_333, 303_331, 3, 20_000L, 210_000L, null),

                escenario("Ciclo vencido: hoy es el día del ingreso",
                        CICLO_OCTUBRE,
                        List.of(gasto(140_000, "2026-10-21")),
                        List.of(),
                        "2026-11-03",
                        460_000, 0, 460_000, 0, 0, 0, 0, 15, 9_333L, 460_000L, null,
                        CICLO_VENCIDO),

                escenario("Ciclo vencido: hoy es después del ingreso, con gasto hoy",
                        CICLO_OCTUBRE,
                        List.of(gasto(140_000, "2026-10-21"), gasto(20_000, "2026-11-05")),
                        List.of(),
                        "2026-11-05",
                        440_000, 0, 440_000, 0, 0, 0, 0, 17, 9_411L, 440_000L, null,
                        CICLO_VENCIDO),

                escenario("Ingresos dentro del ciclo suman a disponible",
                        CICLO_OCTUBRE,
                        List.of(gasto(120_000, "2026-10-21"), ingreso(90_000, "2026-10-23")),
                        List.of(),
                        "2026-10-25",
                        570_000, 0, 570_000, 9, 63_333, 63_333, 443_331, 6, 20_000L, 390_000L, null),

                escenario("Gasto de hoy supera el cupo: cupoHoy negativo",
                        CICLO_OCTUBRE,
                        List.of(gasto(90_000, "2026-10-25")),
                        List.of(),
                        "2026-10-25",
                        510_000, 0, 510_000, 9, 66_666, -23_334, 376_662, 6, 15_000L, 375_000L, null),

                escenario("Menos de 7 días restantes: la semana usa diasRestantes",
                        CICLO_OCTUBRE,
                        List.of(gasto(400_000, "2026-10-22")),
                        List.of(),
                        "2026-10-30",
                        200_000, 0, 200_000, 4, 50_000, 50_000, 200_000, 11, 36_363L, 54_548L, null),

                escenario("Pago de compromiso hoy: no cuenta en gastosHoy ni en el ritmo",
                        CICLO_OCTUBRE,
                        List.of(gasto(60_000, "2026-10-22"), pagoCompromiso(50_000, "2026-10-25")),
                        List.of(pagado(50_000)),
                        "2026-10-25",
                        490_000, 0, 490_000, 9, 54_444, 54_444, 381_108, 6, 10_000L, 400_000L, null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("estadosAntesDePagar")
    void pagarUnCompromisoNoCambiaLibre(String nombre, DatosCiclo ciclo, List<MovimientoPlan> movimientos,
                                       long montoCompromiso, LocalDate hoy) {
        ResumenPlan antes = calculator.calcular(ciclo, movimientos, List.of(pendiente(montoCompromiso)), hoy);

        List<MovimientoPlan> conPago = new ArrayList<>(movimientos);
        conPago.add(new MovimientoPlan(TipoMovimiento.GASTO, montoCompromiso, hoy, true));
        ResumenPlan despues = calculator.calcular(ciclo, conPago, List.of(pagado(montoCompromiso)), hoy);

        assertThat(despues.libre()).isEqualTo(antes.libre());
        assertThat(despues).isEqualTo(new ResumenPlan(
                antes.hoy(), antes.fechaProximoIngreso(), antes.fechaEstimada(),
                antes.disponible() - montoCompromiso, antes.comprometido() - montoCompromiso,
                antes.libre(), antes.diasRestantes(), antes.cupoDiario(), antes.cupoHoy(),
                antes.cupoSemana(), antes.diasTranscurridos(), antes.ritmoDiario(),
                antes.saldoProyectado(), antes.fechaAgotamiento(), antes.alertas()));
    }

    static Stream<Arguments> estadosAntesDePagar() {
        return Stream.of(
                Arguments.of("Caso A: pagar el celular", CICLO_OCTUBRE,
                        List.of(gasto(120_000, "2026-10-21"), gasto(80_000, "2026-10-23"), gasto(15_000, "2026-10-25")),
                        50_000L, fecha("2026-10-25")),
                Arguments.of("Caso C: pagar el arriendo", ciclo(200_000, "2026-10-24", "2026-11-15"),
                        List.of(gasto(150_000, "2026-10-24")),
                        100_000L, fecha("2026-10-25")),
                Arguments.of("Ciclo vencido: pagar un compromiso", CICLO_OCTUBRE,
                        List.of(gasto(140_000, "2026-10-21")),
                        70_000L, fecha("2026-11-04"))
        );
    }

    // --- Ayudas para construir escenarios ---

    private static Arguments escenario(String nombre, DatosCiclo ciclo, List<MovimientoPlan> movimientos,
                                       List<CompromisoPlan> compromisos, String hoy,
                                       long disponible, long comprometido, long libre, int diasRestantes,
                                       long cupoDiario, long cupoHoy, long cupoSemana, int diasTranscurridos,
                                       Long ritmoDiario, Long saldoProyectado, String fechaAgotamiento,
                                       CodigoAlerta... alertas) {
        ResumenPlan esperado = new ResumenPlan(fecha(hoy), ciclo.fechaProximoIngreso(), ciclo.fechaEstimada(),
                disponible, comprometido, libre, diasRestantes, cupoDiario, cupoHoy, cupoSemana,
                diasTranscurridos, ritmoDiario, saldoProyectado,
                fechaAgotamiento == null ? null : fecha(fechaAgotamiento),
                Arrays.stream(alertas).map(Alerta::de).toList());
        return Arguments.of(nombre, ciclo, movimientos, compromisos, fecha(hoy), esperado);
    }

    private static DatosCiclo ciclo(long saldoInicial, String inicio, String proximoIngreso) {
        return new DatosCiclo(saldoInicial, fecha(inicio), fecha(proximoIngreso), false);
    }

    private static MovimientoPlan gasto(long monto, String fecha) {
        return new MovimientoPlan(TipoMovimiento.GASTO, monto, fecha(fecha), false);
    }

    private static MovimientoPlan pagoCompromiso(long monto, String fecha) {
        return new MovimientoPlan(TipoMovimiento.GASTO, monto, fecha(fecha), true);
    }

    private static MovimientoPlan ingreso(long monto, String fecha) {
        return new MovimientoPlan(TipoMovimiento.INGRESO, monto, fecha(fecha), false);
    }

    private static CompromisoPlan pendiente(long monto) {
        return new CompromisoPlan(monto, EstadoCompromiso.PENDIENTE);
    }

    private static CompromisoPlan pagado(long monto) {
        return new CompromisoPlan(monto, EstadoCompromiso.PAGADO);
    }

    private static LocalDate fecha(String iso) {
        return LocalDate.parse(iso);
    }
}
