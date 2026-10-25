-- PC-05: esquema inicial. Dinero en pesos enteros (BIGINT).

CREATE TABLE usuario (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    nombre     VARCHAR(100) NOT NULL,
    google_sub VARCHAR(255) UNIQUE
);

CREATE TABLE ciclo (
    id                    BIGSERIAL   PRIMARY KEY,
    usuario_id            BIGINT      NOT NULL REFERENCES usuario (id),
    saldo_inicial         BIGINT      NOT NULL CHECK (saldo_inicial >= 0),
    fecha_inicio          DATE        NOT NULL,
    fecha_proximo_ingreso DATE        NOT NULL,
    fecha_estimada        BOOLEAN     NOT NULL DEFAULT FALSE,
    estado                VARCHAR(10) NOT NULL CHECK (estado IN ('ACTIVO', 'CERRADO'))
);

-- Un solo ciclo ACTIVO por usuario.
CREATE UNIQUE INDEX ux_ciclo_activo_por_usuario ON ciclo (usuario_id) WHERE estado = 'ACTIVO';

CREATE TABLE compromiso (
    id       BIGSERIAL    PRIMARY KEY,
    ciclo_id BIGINT       NOT NULL REFERENCES ciclo (id),
    nombre   VARCHAR(100) NOT NULL,
    monto    BIGINT       NOT NULL CHECK (monto > 0),
    estado   VARCHAR(10)  NOT NULL CHECK (estado IN ('PENDIENTE', 'PAGADO'))
);

CREATE INDEX ix_compromiso_ciclo ON compromiso (ciclo_id);

CREATE TABLE movimiento (
    id            BIGSERIAL    PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario (id),
    ciclo_id      BIGINT       NOT NULL REFERENCES ciclo (id),
    tipo          VARCHAR(10)  NOT NULL CHECK (tipo IN ('GASTO', 'INGRESO')),
    monto         BIGINT       NOT NULL CHECK (monto > 0),
    categoria     VARCHAR(20)  NOT NULL,
    fecha         DATE         NOT NULL,
    descripcion   VARCHAR(140),
    origen        VARCHAR(10)  NOT NULL CHECK (origen IN ('MANUAL', 'TEXTO', 'VOZ')),
    compromiso_id BIGINT       REFERENCES compromiso (id),
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Listado del ciclo: más reciente primero (fecha, luego creado_en).
CREATE INDEX ix_movimiento_ciclo_fecha ON movimiento (ciclo_id, fecha DESC, creado_en DESC);
CREATE INDEX ix_movimiento_usuario ON movimiento (usuario_id);
