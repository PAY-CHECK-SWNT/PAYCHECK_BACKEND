package com.paycheck.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void relojUsaZonaBogota() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("America/Bogota"));
    }
}
