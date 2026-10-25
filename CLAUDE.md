# Pay&Check

App para universitarios colombianos: registran gastos e ingresos y la app calcula un plan
hasta su próximo ingreso (cuánto tienen libre, cupo de hoy y de la semana, alertas).
Proyecto académico, 3 sprints, entrega el 17 de noviembre de 2026.

## Principio central
- El LLM solo **interpreta** texto del usuario y **redacta** explicaciones.
- Todas las cifras las calcula `PlanCalculator` con reglas explícitas (`docs/reglas-plan.md`).
- Nunca pedirle al LLM que calcule montos ni que decida.

## Stack
- `backend/`: Java 17, Spring Boot 3, Maven wrapper, Spring Data JPA, Flyway, PostgreSQL 16 (docker-compose), JUnit 5.
- `web/`: React + Vite + TypeScript, Tailwind, React Router. Mobile-first (se usa desde el celular).
- IA (Sprint 2): Groq, clave en variable de entorno `GROQ_API_KEY`.

## Comandos
- Backend: `cd backend && docker compose up -d && ./mvnw spring-boot:run` · pruebas: `./mvnw test`
- Web: `cd web && npm install && npm run dev -- --host` · `npm run build` · `npm run lint`

## Estructura
- Backend por funcionalidad: `com.paycheck.{ciclo, movimiento, compromiso, plan, resumen, auth, ia, common}`.
  Cada paquete: entidad, repositorio, servicio, controlador, DTOs.
- `plan/PlanCalculator`: clase pura, sin Spring ni base de datos; recibe la fecha de hoy como parámetro.
- Web: `src/features/{ciclo, movimientos, panel}`, cliente HTTP en `src/api/`.
  Si `VITE_USE_MOCKS=true`, el cliente devuelve datos simulados con la forma exacta de `docs/api.md`.

## Modelo de datos
- `usuario`: id, email, nombre, google_sub (nullable hasta Sprint 2)
- `ciclo`: id, usuario_id, saldo_inicial, fecha_inicio, fecha_proximo_ingreso, fecha_estimada, estado (ACTIVO | CERRADO). Un solo ACTIVO por usuario.
- `movimiento`: id, usuario_id, ciclo_id, tipo (GASTO | INGRESO), monto, categoria, fecha, descripcion, origen (MANUAL | TEXTO | VOZ), compromiso_id (nullable), creado_en
- `compromiso`: id, ciclo_id, nombre, monto, estado (PENDIENTE | PAGADO)

## Convenciones
- Dinero: pesos colombianos enteros, tipo `long` / `BIGINT`. Nunca `double`.
- Fechas: `LocalDate`, zona `America/Bogota`, formato ISO `yyyy-MM-dd` en JSON.
- Nombres del dominio en español (Ciclo, Movimiento, Compromiso); código y comentarios claros y breves.
- El usuario actual se obtiene solo con `CurrentUserProvider`. En el perfil `dev` es un usuario demo fijo;
  en Sprint 2 se reemplaza por login con Google sin tocar los controladores.
- Errores con el formato de `docs/api.md`. Mensajes para el usuario en español.

## Reglas para Claude
- El contrato es `docs/api.md`. No cambies endpoints ni JSON sin preguntarme primero.
- Las fórmulas están en `docs/reglas-plan.md`. Si algo es ambiguo, pregunta; no inventes reglas.
- Trabaja solo en la carpeta de la tarea (`backend/` o `web/`).
- No agregues dependencias nuevas sin preguntarme.
- Toda lógica de cálculo lleva pruebas. Al terminar una tarea, corre las pruebas y muestra el resultado.
- Nunca escribas claves ni secretos en el código; usa variables de entorno y `.env` (ignorado por git).
- Mensajes de commit: empiezan con la tarea de Jira, por ejemplo `PC-19: agrega PlanCalculator`.
