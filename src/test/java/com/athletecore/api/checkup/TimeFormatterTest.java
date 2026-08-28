package com.athletecore.api.checkup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de TimeFormatter: conversión bidireccional entre segundos
 * decimales y formato mm:ss.ms (hh:mm:ss.ms para tiempos >= 1 hora), deltas con
 * signo y tolerancia a null.
 */
class TimeFormatterTest {

    @Test
    @DisplayName("Debe formatear 65.250 segundos como 01:05.250")
    void toFormatted_65_250_devuelve01_05_250() {
        assertEquals("01:05.250", TimeFormatter.toFormatted(new BigDecimal("65.250")));
    }

    @Test
    @DisplayName("Debe formatear 125.755 segundos como 02:05.755")
    void toFormatted_125_755_devuelve02_05_755() {
        assertEquals("02:05.755", TimeFormatter.toFormatted(new BigDecimal("125.755")));
    }

    @Test
    @DisplayName("Debe formatear 3600 segundos como 01:00:00.000 (formato con horas)")
    void toFormatted_3600_devuelve01_00_00_000() {
        assertEquals("01:00:00.000", TimeFormatter.toFormatted(new BigDecimal("3600")));
    }

    @Test
    @DisplayName("Debe devolver null cuando la entrada es null")
    void toFormatted_null_devuelveNull() {
        assertNull(TimeFormatter.toFormatted(null));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException cuando el tiempo es negativo")
    void toFormatted_negativo_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> TimeFormatter.toFormatted(new BigDecimal("-2.000")));
    }

    @Test
    @DisplayName("Debe formatear un delta negativo con signo menos")
    void toSignedFormatted_devuelveConSignoNegativo_cuandoDiffNegativo() {
        assertEquals("-00:02.000", TimeFormatter.toSignedFormatted(new BigDecimal("-2.000")));
    }

    @Test
    @DisplayName("Debe formatear un delta positivo con signo más")
    void toSignedFormatted_devuelveConSignoPositivo_cuandoDiffPositivo() {
        assertEquals("+00:02.000", TimeFormatter.toSignedFormatted(new BigDecimal("2.000")));
    }

    @Test
    @DisplayName("Debe convertir 01:05.250 de vuelta a 65.250 segundos")
    void toSeconds_01_05_250_devuelve65_250() {
        assertEquals(new BigDecimal("65.250"), TimeFormatter.toSeconds("01:05.250"));
    }

    @Test
    @DisplayName("Debe convertir 01:00:00.000 a 3600.000 segundos")
    void toSeconds_01_00_00_000_devuelve3600() {
        assertEquals(new BigDecimal("3600.000"), TimeFormatter.toSeconds("01:00:00.000"));
    }

    @Test
    @DisplayName("Debe devolver null cuando la entrada es null o vacía")
    void toSeconds_null_devuelveNull() {
        assertNull(TimeFormatter.toSeconds(null));
        assertNull(TimeFormatter.toSeconds("  "));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException con formato inválido")
    void toSeconds_formatoInvalido_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TimeFormatter.toSeconds("abc"));
        assertThrows(IllegalArgumentException.class, () -> TimeFormatter.toSeconds("99:99"));
    }

    @Test
    @DisplayName("Debe ser reversible el round-trip segundos → formato → segundos")
    void roundTrip_segundosFormatoSegundos_conservaElValor() {
        BigDecimal original = new BigDecimal("125.755");

        BigDecimal roundTrip = TimeFormatter.toSeconds(TimeFormatter.toFormatted(original));

        assertEquals(original, roundTrip);
    }
}
